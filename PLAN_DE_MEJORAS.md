# Plan de Mejora — Corralón UTN (Backend)

> Documento de propuesta de mejoras para el backend del ecommerce de corralón.
> Deriva del análisis del código fuente y de `REGLAS_DE_NEGOCIO.md`.
> Última actualización: Agosto 2026

---

## Índice

1. [Visión general y estado actual](#1-visión-general-y-estado-actual)
2. [Bugs a corregir (prioridad alta)](#2-bugs-a-corregir-prioridad-alta)
3. [Requerimientos solicitados](#3-requerimientos-solicitados)
   - [3.1 Paginación](#31-paginación)
   - [3.2 Devolver objeto en un pago](#32-devolver-objeto-en-un-pago)
   - [3.3 API de mapa para dirección (OpenStreetMap)](#33-api-de-mapa-para-dirección-openstreetmap)
   - [3.4 API de imágenes (Cloudinary)](#34-api-de-imágenes-cloudinary)
4. [Mejoras por feature](#4-mejoras-por-feature)
   - [4.1 Notificaciones y correo](#41-notificaciones-y-correo)
   - [4.2 Órdenes](#42-órdenes)
   - [4.3 Pagos](#43-pagos)
   - [4.4 Stock y movimientos](#44-stock-y-movimientos)
   - [4.5 Precios](#45-precios)
   - [4.6 Seguridad y autenticación](#46-seguridad-y-autenticación)
   - [4.7 Ofertas y promociones](#47-ofertas-y-promociones)
5. [Mejoras transversales](#5-mejoras-transversales)
6. [Ideas extra](#6-ideas-extra)
7. [Roadmap sugerido](#7-roadmap-sugerido)

---

## 1. Visión general y estado actual

El sistema es una plataforma de **ecommerce** para la venta de materiales de construcción (corralón).

**Stack:** Spring Boot 4.0.6 · Java 21 · MySQL 8 · Spring Security + JWT (jjwt 0.13.0) · ModelMapper 3.2.1 · springdoc-openapi · Spring Mail · spring-dotenv.

**Arquitectura:** monolito modular organizado por *features* (`features/order`, `features/productVariant`, `features/payment`, etc.), cada una con el patrón `controller → service → repository` más `entity`, `dto` y `mapper`. Los controllers exponen DTOs (nunca entidades), el mapeo se hace con ModelMapper y la eliminación se realiza mediante **baja lógica** (campo `active`). Cada entidad expone un `externalId` de tipo `UUID` (la clave primaria `id` interna no se filtra por la API).

**Fortalezas a preservar:**

- Separación correcta de capas y DTOs en toda la frontera (sin fuga de entidades).
- Baja lógica para trazabilidad en producto, variante y usuario.
- Trazabilidad de stock mediante `StockMovementEntity` (invariante #6 cumplido).
- Envío de correo asíncrono con ejecutor dedicado y errores silenciosos.
- Docker Compose con healthcheck y Mailhog para desarrollo.
- `externalId` UUID en toda la API.

**Debilidad central:** el problema no es la arquitectura, sino la **madurez** del código — concurrencia de stock sin control, ausencia de tests, lógica duplicada, permisos definidos pero sin endpoints que los usen, y funcionalidades a medio construir (refresh token, notificaciones in-app, catálogo público).

---

## 2. Bugs a corregir (prioridad alta)

| # | Bug | Ubicación | Detalle |
|---|---|---|---|
| 1 | `ProductService.activate()` roto | `features/product/service/ProductService.java` | Condición invertida: `if (!product.isActive()) throw "already active"`. Nunca se puede reactivar un producto inactivo. |
| 2 | `OfferController.addProduct` ignora `@PathVariable` | `features/offer/controller/OfferController.java` | Usa `offerId` del DTO en lugar del de la URL. |
| 3 | `ProductController.update` devuelve 201 | `features/product/controller/ProductController.java` | Una actualización (PUT) debe devolver 200, no 201. |
| 4 | `CartResponseDTO.userExternalId` siempre `null` | `features/cart/service/CartServiceImpl.java` | El campo nunca se mapea ni se setea manualmente. |
| 5 | Cancelación/gestión de órdenes por EMPLOYEE inalcanzable | `features/order/controller/OrderController.java` | `ORDER_CANCEL_ANY` y `ORDER_MANAGE_STATUS` no tienen endpoint que los use. |
| 6 | Sin `/refresh`, sin forgot-password, sin admin seedeado | `features/auth/` | `RefreshTokenRequest` y `CredentialsEntity.refreshToken` son código muerto. |
| 7 | `AddressController` sin slash inicial | `features/address/controller/AddressController.java` | `@RequestMapping("api/addresses")` en vez de `/api/addresses`. |
| 8 | `NotificationEntity` huérfana | `features/notifications/` | Ningún evento de negocio crea notificaciones in-app; solo se envían correos. El flag `sent` nunca se pone en `true`. |
| 9 | `System.out.println` de debug | `CredentialsEntity.getAuthorities()`, `SecurityDataInitializer` | Ruido y posible fuga de información en cada autenticación. |

---

## 3. Requerimientos solicitados

### 3.1 Paginación

**Estado actual:** ningún listado pagina. Todos los endpoints de listado/búsqueda devuelven `List<T>` (`findAll`, `search`, listados `inactive`, órdenes de admin, etc.).

**Propuesta:**

1. El controller recibe `Pageable pageable` (Spring lo construye desde `?page=0&size=20&sort=name,asc`).
2. El service devuelve `Page<T>`.
3. No exponer `Page<T>` directamente (serializa ruido y acopla el front a Spring Data). Crear un envoltorio propio:

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
```

```java
@GetMapping("/search")
@PreAuthorize("hasAuthority('PRODUCT_LIST')")
public PageResponse<ProductResponseDTO> search(ProductFilterDTO filter, Pageable pageable) {
    return productService.search(filter, pageable);
}
```

4. Validar `size` máximo (por ejemplo 100) y definir un valor por defecto (20) para evitar `size=100000`.

**Alcance:** `products/search`, `variants/search`, listados `inactive`, `orders/admin`, `users`, `addresses`, `stock-movements`, `offers`, `notifications`, `payments`.

---

### 3.2 Devolver objeto en un pago

**Estado actual:** el endpoint `POST /api/payments/pay` **ya devuelve un objeto** (`ResponseEntity<PaymentResponseDTO>`). Este punto puede referirse a una versión anterior del código o al hecho de que **los errores devuelven strings crudos**.

**Propuesta (reforzar y cerrar las brechas reales):**

1. **Enriquecer `PaymentResponseDTO`** para que el front no tenga que consultar otro endpoint para saber el resultado completo:

```json
{
  "paymentId": "...",
  "orderId": "...",
  "amount": 1500.00,
  "paymentMethod": "CREDIT_CARD",
  "paymentStatus": "APPROVED",
  "orderStatus": "PAID",
  "createdAt": "2026-08-24T10:30:00"
}
```

2. **Envolver el único retorno primitivo suelto:** `GET /product-variants/{id}/stock` devuelve hoy `ResponseEntity<Integer>`. Debe devolver un objeto `{ "stock": 5 }`.

3. **Eliminar los `ResponseEntity<String>` de errores** (ver §5, `ErrorResponse`). El objetivo final: **ningún endpoint devuelve un string plano**, ni en éxito ni en error.

---

### 3.3 API de mapa para dirección (OpenStreetMap)

**Estado actual:** `AddressEntity` tiene `street`, `streetNumber`, `floor`, `apartmentNumber`, `city` y `zipCode`. **No tiene provincia, ni lat/lng, ni geocoding.** No hay soporte de coordenadas en todo el repositorio.

**Propuesta (geocoding con Nominatim):**

1. **Campos nuevos en `AddressEntity`:**

```java
@Column(name = "province")
private String province;

@Column(name = "latitude")
private Double latitude;

@Column(name = "longitude")
private Double longitude;

@Column(name = "display_name", length = 512)
private String displayName;
```

2. **Llamada de geocoding (búsqueda estructurada):**

```
GET https://nominatim.openstreetmap.org/search
    ?street=<nº+calle>&city=<ciudad>&state=<provincia>
    &postalcode=<cp>&countrycodes=ar&format=jsonv2&limit=1&accept-language=es
```

La respuesta incluye `lat`, `lon`, `display_name` y, con `addressdetails=1`, el desglose por componentes.

3. **Flujo:** al crear/actualizar una dirección, se geocodifica y se persisten `latitude`/`longitude`/`province`/`displayName`. **Degradación elegante:** si Nominatim no responde o falla, la dirección se guarda igual con coordenadas `null` — un servicio externo no debe bloquear el alta de una dirección.

4. **Autocomplete para el front:** endpoint `GET /api/addresses/geocode/suggest?q=` que devuelve candidatos (`display_name`, `lat`, `lon`) mientras el usuario tipea.

5. **Cumplir la usage policy de OSM:**
   - `User-Agent` identificable y configurable.
   - Filtro `countrycodes=ar`.
   - Caché de resultados y rate limiting.
   - Para volumen alto, instancia propia de Nominatim (self-hosted).

6. **Front (Angular):** Leaflet + tiles de OpenStreetMap para mostrar el pin y permitir elegir el punto en el mapa.

**Valor:** permite calcular costo de envío por zona, distancia, y "sucursal más cercana" para retiro.

---

### 3.4 API de imágenes (Cloudinary)

**Estado actual:** ni `ProductEntity` ni `ProductVariantEntity` tienen campo de imagen. Cero soporte hoy.

**Propuesta:**

1. **Modelo de datos.** Las variantes son lo que se vende; el producto es lo que se muestra. Opción recomendada: entidad `ProductImage` para soportar varias fotos por producto:

```java
@Entity
public class ProductImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String publicId;      // identificador en Cloudinary

    @Column(nullable = false)
    private String secureUrl;     // URL https de Cloudinary

    @Column(nullable = false)
    private Integer position;     // orden de visualización

    @Column(nullable = false)
    private Boolean primary = false; // imagen principal

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
}
```

> Alternativa más simple si con una foto alcanza: columnas `imagePublicId`/`imageUrl` directamente en `ProductEntity`.

2. **Upload firmado desde el backend** (recomendado para admin/employee):

```java
Map<?, ?> result = cloudinary.uploader().upload(
        file.getBytes(),
        ObjectUtils.asMap(
                "public_id", "products/" + productId + "/image-" + position,
                "folder", "products",
                "resource_type", "image"
        ));

String publicId  = (String) result.get("public_id");
String secureUrl = (String) result.get("secure_url");
```

Se persiste el `public_id` (no la URL entera) y las URLs se derivan con transformaciones.

3. **Transformaciones** para no servir imágenes gigantes:
   - Catálogo: `f_auto,q_auto,w_400`
   - Detalle/zoom: `w_1200`
   - Miniatura: `w_200,h_200,c_fill`

4. **Alternativa:** upload unsigned desde Angular con `upload_preset` (el front sube directo a Cloudinary y envía el `public_id` al backend). Más simple, menos control. Para la tesis, el upload firmado desde backend es más robusto y demuestra mejor criterio.

5. Dependencia: `com.cloudinary:cloudinary-http44`.

---

## 4. Mejoras por feature

### 4.1 Notificaciones y correo

**Estado actual:** `EmailService` envía correos `@Async` **dentro de la transacción**, y `NotificationEntity` (notificaciones in-app) está desconectada de los eventos de negocio.

**Problema:** un correo puede enviarse aunque la transacción luego haga rollback (ej. "correo de orden creada" para una orden que no se persiste). Además, la tabla de notificaciones existe pero nadie la alimenta.

**Propuesta — arquitectura orientada a eventos:**

1. Los servicios publican **eventos de dominio** con `ApplicationEventPublisher` (`OrderCreated`, `OrderPaid`, `OrderCancelled`, `UserRegistered`, `PasswordChanged`, `NewPromotion`).

```java
public record OrderCreatedEvent(
        UUID orderExternalId,
        String email,
        String customerName,
        OrderResponseDTO order
) {}
```

2. Un listener corre **después del commit** y de forma asíncrona:

```java
@Component
public class OrderNotificationListener {

    private final NotificationService notificationService;
    private final IEmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailTaskExecutor")
    public void onOrderCreated(OrderCreatedEvent event) {
        // 1) persistir notificación in-app
        notificationService.create(event.customerName(), NotificationType.ORDER_CREATED, "...");
        // 2) enviar correo
        emailService.sendOrderCreatedEmail(event.email(), event.customerName(), event.order());
    }
}
```

**Beneficios:**
- El correo sale **solo si la transacción commitea** (se elimina el "correo de orden fantasma").
- La notificación in-app queda conectada a los eventos de negocio (deja de estar huérfana).
- Un único punto decide "qué pasa" ante cada evento.

3. **Plantillas HTML con Thymeleaf** (el `REGLAS_DE_NEGOCIO.md` ya lo deja como objetivo futuro).
4. **Resiliencia:** retry suave y logging, sin propagar excepciones al flujo principal (comportamiento actual a preservar).

---

### 4.2 Órdenes

**Estado actual:** flujo plano con tres estados (`PENDING_PAYMENT`, `PAID`, `CANCELLED`) y permisos definidos (`ORDER_MANAGE_STATUS`) sin endpoint que los use.

**Propuesta — máquina de estados explícita:**

1. **Transiciones permitidas** declaradas y centralizadas:

```
PENDING_PAYMENT → PAID
PENDING_PAYMENT → CANCELLED
```

Si se desea que EMPLOYEE gestione el ciclo completo, agregar los estados que el enum `NotificationType` ya sugiere (`ORDER_CONFIRMED`, `ORDER_PREPARING`, `ORDER_COMPLETED`) y sus endpoints con `ORDER_MANAGE_STATUS`. De lo contrario, **eliminar los valores muertos** del enum para no dejar un contrato que no se cumple.

2. **Número de orden legible** además del UUID (`ORD-2026-000123`) para correos y soporte.

3. **Idempotencia en checkout** para evitar que un doble submit cree dos órdenes.

4. **Historial de estados** (`OrderStatusHistory`: estado, timestamp, actor) para auditoría.

5. **Habilitar cancelación por EMPLOYEE/ADMIN** usando `ORDER_CANCEL_ANY` (hoy inalcanzable).

---

### 4.3 Pagos

**Estado actual:** `PaymentService.pay()` usa `simulateCard()` con `Math.random()`. El enum `MERCADO_PAGO` existe pero no hay integración. Un segundo pago sobre la misma orden rompe con violación de unique-constraint (HTTP 500).

**Propuesta:**

1. **Abstracción de gateway** (patrón Strategy):

```java
public interface PaymentGateway {
    PaymentResult process(PaymentRequestDTO request);
}
```

Implementaciones: `SimulatedCardGateway`, `MercadoPagoGateway`, `TransferGateway`.

2. **Guard de pago duplicado:** si ya existe un pago para la orden, devolver el existente en lugar de lanzar error.

3. **Idempotencia** con clave por request.

4. **`MERCADO_PAGO`** como integración real (el gancho natural para un corralón argentino).

---

### 4.4 Stock y movimientos

**Estado actual:** el descuento de stock es un patrón *read → check → write* sin locking. Dos requests concurrentes pueden leer el mismo stock, ambos pasar el `if`, y terminar con **stock negativo** (viola el invariante #1).

**Propuesta (obligatoria):**

1. **Locking optimista** en `ProductVariantEntity`:

```java
@Version
@Column(nullable = false)
private Long version;
```

Y manejar `OptimisticLockException` con reintento o con respuesta 409.

2. **O bien, update atómico con guard** (más robusto para este caso):

```java
@Modifying
@Query("""
    UPDATE ProductVariantEntity v
       SET v.stock = v.stock - :qty
     WHERE v.externalId = :id
       AND v.stock >= :qty
    """)
int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

// si retorna 0 filas → StockInsufficientException
```

3. **Extraer un `StockService` único.** Hoy `restoreStock` y `createStockMovement` están duplicados en `OrderService`, `PaymentService` y `ProductVariantService`.

4. **Opcional avanzado:** stock reservado en `PENDING_PAYMENT` con timeout (si no se paga en X minutos, se libera).

---

### 4.5 Precios

**Estado actual:** `calculateUnitPrice` está duplicada en `OrderService` y `CartServiceImpl`. La lógica de oferta usa `discountedPrice` mientras `discountPercentage` queda muerto (dos mecanismos paralelos, solo uno se aplica).

**Propuesta:**

1. **Un único `PricingService`** con la cadena de decisión:

```
oferta activa → precio mayorista (si qty ≥ min) → precio minorista
```

2. **Elegir un modelo de oferta.** Recomendado: calcular `discountedPrice = price * (1 - discountPercentage / 100)` y eliminar la ambigüedad. Si se mantiene `discountedPrice` manual, quitar `discountPercentage` del modelo para no tener dos fuentes de verdad.

---

### 4.6 Seguridad y autenticación

**Estado actual:** todo requiere autenticación salvo `/api/auth/**` y swagger. El modelo de autorización mezcla `hasAuthority` (permisos) y `hasAnyRole` (roles). No hay refresh token, ni forgot-password, ni admin por defecto.

**Propuesta:**

1. **Catálogo público:** `permitAll()` en los GET de productos, variantes, categorías, marcas y ofertas, para que el rol GUEST pueda navegar (hoy contradice el propio `REGLAS_DE_NEGOCIO.md`).

2. **Un modelo de autorización.** Elegir permisos finos (`hasAuthority`) o roles (`hasRole`), no ambos mezclados. Recomendado: permisos finos para endpoints y roles como agrupación grosera.

3. **Refresh token real:** endpoint `/api/auth/refresh` con rotación y revocación (hoy `CredentialsEntity.refreshToken` es código muerto).

4. **Forgot/reset password** con token por correo.

5. **Seed de un usuario ADMIN por defecto** (hoy solo se siembran roles y permisos).

6. **Eliminar `System.out.println`** y usar `@Slf4j`.

---

### 4.7 Ofertas y promociones

**Estado actual:** `OfferController.addProduct` ignora el `@PathVariable`. La activación es manual.

**Propuesta:**

1. Corregir el bug del `@PathVariable`.

2. **Activación automática por fechas** (cron): la oferta "vive" según `startDate`/`endDate` sin depender de un toggle manual.

3. Resolver la inconsistencia `discountPercentage` vs `discountedPrice` (ver §4.5).

---

## 5. Mejoras transversales

1. **Unificar `@Transactional`.** Hoy se mezcla `jakarta.transaction.Transactional` (Product, ProductVariant, Order) con `org.springframework.transaction.annotation.Transactional` (el resto). Usar exclusivamente la de Spring.

2. **`ErrorResponse` unificado.** Sustituir los `ResponseEntity<String>`:

```java
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors
) {}
```

Agregar handlers para `Exception` genérica y `MethodArgumentTypeMismatchException`.

3. **Migraciones con Flyway** en lugar de `ddl-auto: update`.

4. **Tests.** Unit + integración de los invariantes: stock no negativo, precio congelado al crear la orden, cancelación devuelve stock, precio mayorista, flujo de pago. Hoy solo existe `contextLoads()`.

5. **Arreglar N+1:** relaciones `@ManyToOne` EAGER en `ProductEntity`; búsqueda de credenciales por usuario en `UserService.getAll` y `OfferService.activate`.

6. **Limpiar código muerto:** `RefreshTokenRequest`, `OrderRequestDTO`, `OrderItemRequestDTO`, mappers sin uso, imports sin uso (`LockModeType`, `@Lock`), `PaymentRepository.findByExternalIdWithOrder`.

---

## 6. Ideas extra

Ideas que aportan valor real más allá de los requerimientos, listadas de forma breve:

1. **Integración MercadoPago** — gateway de pago real, el más relevante para un ecommerce argentino.
2. **Costo de envío por zona** — usando las coordenadas de Nominatim para calcular tarifa por distancia/sucursal.
3. **Observabilidad** — Spring Actuator + logs estructurados.
4. **Cache del catálogo** — Caffeine/Redis para búsquedas.
5. **Rate limiting y versionado de API** — `/api/v1/...` con límites por cliente.
6. **Auditoría** — registro de quién cambió qué y cuándo (muy valioso para la defensa de tesis).
7. **Import masivo CSV** — carga inicial de productos y stock.

---

## 7. Roadmap sugerido

| Fase | Objetivo | Contenido |
|---|---|---|
| **0 — Estabilizar** | Corregir bugs y deuda crítica | Bugs de §2, unificar `@Transactional`, `ErrorResponse`, Flyway. |
| **1 — Valor de negocio** | Robustez del núcleo | Stock con locking, `PricingService`, máquina de estados de órdenes, cancelación admin, correo event-driven + notificaciones. |
| **2 — Requerimientos** | Lo solicitado | Paginación, enriquecer pago, geocoding OSM, imágenes Cloudinary. |
| **3 — Extras** | Funciones de producto | Refresh token, reset password, MercadoPago, costo por zona. |
| **4 — Tesis** | Calidad y documentación | Tests, observabilidad, documentación final. |
