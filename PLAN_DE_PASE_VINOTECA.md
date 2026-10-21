# Plan de Pase — Corralón UTN → Vinoteca

> Documento de migración del ecommerce de corralón a un ecommerce de vinoteca.
> Deriva del estado real del código fuente (verificado archivo por archivo), de
> `REGLAS_DE_NEGOCIO.md` y de `PLAN_DE_MEJORAS.md`.
>
> Propósito: permitir que un equipo haga un fork del repositorio y ejecute el pase
> sin rediseñar el dominio, tocando la menor cantidad de entidades posible y
> mejorando la delegación de responsabilidades (SOLID/GRASP) sin abandonar Spring Boot.
>
> Última actualización: Octubre 2026

---

## Índice

1. [Cómo usar este documento](#1-cómo-usar-este-documento)
   - 1.1 Nomenclatura del dominio
   - 1.2 Estrategia de fork
   - 1.3 Convención de ramas
   - 1.4 Regla de oro
2. [Resumen ejecutivo](#2-resumen-ejecutivo)
3. [Estado actual del sistema](#3-estado-actual-del-sistema)
   - 3.1 Stack y dependencias
   - 3.2 Estructura de paquetes
   - 3.3 Convenciones transversales
   - 3.4 Modelo de datos completo
   - 3.5 Seguridad y autorización
   - 3.6 Inventario funcional por módulo
   - 3.7 Flujos de negocio clave
   - 3.8 Reglas de negocio e invariantes
   - 3.9 Deuda técnica y bugs conocidos
   - 3.10 Funcionalidades proyectadas
   - 3.11 Configuración y entorno
4. [Dominio vinoteca](#4-dominio-vinoteca)
   - 4.1 Conceptos del negocio
   - 4.2 Mapeo corralón → vinoteca
   - 4.3 Alcance del cambio
   - 4.4 Cambios de modelo de datos
   - 4.5 Cambios de API, DTOs y filtros
   - 4.6 Datos semilla
   - 4.7 Reglas de negocio específicas
5. [Arquitectura objetivo](#5-arquitectura-objetivo)
   - 5.1 Principios
   - 5.2 Estructura de carpetas
   - 5.3 Delegación de responsabilidades
   - 5.4 SOLID aplicado
   - 5.5 GRASP aplicado
   - 5.6 Extracciones concretas (antes/después)
   - 5.7 Límites explícitos
   - 5.8 Catálogo de patrones de diseño
6. [Plan de ejecución por fases](#6-plan-de-ejecución-por-fases)
7. [Estrategia de tests](#7-estrategia-de-tests)
8. [Criterios de aceptación](#8-criterios-de-aceptación)
9. [Riesgos y mitigaciones](#9-riesgos-y-mitigaciones)
10. [Decisiones de diseño](#10-decisiones-de-diseño)
11. [Backlog consolidado](#11-backlog-consolidado)
12. [Apéndices](#12-apéndices)

---

## 1. Cómo usar este documento

### 1.1 Nomenclatura del dominio

Este plan adopta **directamente el vocabulario de la vinoteca**. No se usan nombres
genéricos como `ProductEntity`, `BrandEntity`, `CategoryEntity` o
`ProductVariantEntity` en el diseño destino. El repositorio de partida todavía usa
esos nombres; la Fase 1 los renombra de forma mecánica.

| Concepto | Nombre en el repositorio de partida | Nombre destino | Tabla destino |
|---|---|---|---|
| Vino | `ProductEntity` | **`WineEntity`** | `wines` |
| Formato / presentación | `ProductVariantEntity` | **`WineVariantEntity`** | `wine_variants` |
| Bodega | `BrandEntity` | **`WineryEntity`** | `wineries` |
| Tipo de vino | `CategoryEntity` | **`WineTypeEntity`** | `wine_types` |
| Distribuidor | `SupplierEntity` | **`DistributorEntity`** | `distributors` |
| Varietal / uva | *(no existía)* | **`GrapeVarietyEntity`** (nueva) | `grape_varieties` |
| Imagen de vino | *(no existía)* | **`WineImageEntity`** (proyectada) | `wine_images` |
| DTO de alta/edición de vino | `ProductRequestDTO` | **`WineRequestDTO`** | — |
| DTO de respuesta de vino | `ProductResponseDTO` | **`WineResponseDTO`** | — |
| DTO de alta/edición de formato | `ProductVariantRequestDTO` | **`WineVariantRequestDTO`** | — |
| DTO de respuesta de formato | `ProductVariantResponseDTO` | **`WineVariantResponseDTO`** | — |
| DTO de bodega | `BrandRequestDTO` / `BrandResponseDTO` | **`WineryRequestDTO` / `WineryResponseDTO`** | — |
| DTO de tipo | `CategoryRequestDTO` / `CategoryResponseDTO` | **`WineTypeRequestDTO` / `WineTypeResponseDTO`** | — |
| DTO de distribuidor | `SupplierRequestDTO` / `SupplierResponseDTO` | **`DistributorRequestDTO` / `DistributorResponseDTO`** | — |
| Servicio de vino | `ProductService` | **`WineService`** | — |
| Servicio de formato | `ProductVariantService` | **`WineVariantService`** | — |
| Servicio de bodega | `BrandService` | **`WineryService`** | — |
| Servicio de tipo | `CategoryService` | **`WineTypeService`** | — |
| Servicio de distribuidor | `SupplierService` | **`DistributorService`** | — |
| Paquete de vino | `features/product` | **`features/wine`** | — |
| Paquete de formato | `features/productVariant` | **`features/wineVariant`** | — |
| Paquete de bodega | `features/brand` | **`features/winery`** | — |
| Paquete de tipo | `features/category` | **`features/wineType`** | — |
| Paquete de distribuidor | `features/supplier` | **`features/distributor`** | — |
| Paquete de varietal | *(no existía)* | **`features/grapeVariety`** (nuevo) | — |
| Permisos de vino | `PRODUCT_*` | **`WINE_*`** | — |
| Permisos de formato | `PRODUCT_VARIANT_*` | **`WINE_VARIANT_*`** | — |
| Permisos de bodega | `BRAND_*` | **`WINERY_*`** | — |
| Permisos de tipo | `CATEGORY_*` | **`WINE_TYPE_*`** | — |
| Permisos de distribuidor | `SUPPLIER_*` | **`DISTRIBUTOR_*`** | — |
| Endpoint de vinos | `/api/products` | **`/api/wines`** | — |
| Endpoint de formatos | `/api/product-variants` | **`/api/wine-variants`** | — |
| Endpoint de bodegas | `/api/brands` | **`/api/wineries`** | — |
| Endpoint de tipos | `/api/categories` | **`/api/wine-types`** | — |
| Endpoint de distribuidores | `/api/suppliers` | **`/api/distributors`** | — |
| Endpoint de varietales | *(no existía)* | **`/api/grape-varieties`** (nuevo) | — |

**Entidades que conservan su nombre** porque ya expresan un concepto real del
negocio y no son genéricas: `CartEntity`, `CartItemEntity`, `OrderEntity`,
`OrderItemEntity`, `PaymentEntity`, `AddressEntity`, `OfferEntity`,
`OfferProductEntity`, `StockMovementEntity`, `NotificationEntity`, `UserEntity`,
`CredentialsEntity`, `RoleEntity`, `PermitEntity`.

También se renombran los **campos de relación y las colecciones**: `product` →
`wine`, `productVariant` → `wineVariant`, `products` → `wines`,
`productVariants` → `wineVariants`, `brand` → `winery`, `category` → `wineType`,
`supplier` → `distributor`.

> En las secciones siguientes se usan exclusivamente los nombres destino. Cuando
> aparece una ruta de archivo, corresponde a la ruta posterior a la Fase 1.

### 1.2 Estrategia de fork

El pase se ejecuta sobre un fork, no sobre el repositorio original. El repositorio
original queda como referencia histórica del proyecto de corralón.

```bash
# 1. Crear el fork en GitHub (botón Fork) y clonarlo
git clone git@github.com:<organizacion>/<repo>-vinoteca.git
cd <repo>-vinoteca

# 2. Apuntar el remoto original como upstream (solo lectura)
git remote add upstream git@github.com:<organizacion>/<repo>.git

# 3. Crear la rama de trabajo del pase
git checkout -b feat/pase-vinoteca
```

### 1.3 Convención de ramas

| Rama | Contenido |
|---|---|
| `main` | Estado estable (inicialmente idéntico al corralón) |
| `feat/pase-vinoteca` | Rama de integración del pase |
| `feat/fase-0-baseline` | Tests de caracterización |
| `feat/fase-1-renombre` | Renombre mecánico |
| `feat/fase-2-dominio-vino` | Entidad de varietal y campos de vino |
| `feat/fase-3-solid` | Extracciones de responsabilidad |
| `feat/fase-4-features` | Paginación, catálogo público, etc. |

Cada fase se mergea a `feat/pase-vinoteca` solo cuando pasa sus criterios de
aceptación. `feat/pase-vinoteca` se mergea a `main` al finalizar cada fase estable.

### 1.4 Regla de oro

> Ninguna fase puede cambiar el contrato HTTP de otra fase sin documentarlo.
> Si una fase rompe un endpoint, se actualiza este documento y el front en el mismo PR.

---

## 2. Resumen ejecutivo

El sistema actual es un **monolito modular Spring Boot** organizado por *features*.
El dominio real no es "corralón": es un **comercio con catálogo, formatos con stock
y precio, carrito, órdenes, pagos y envío**. El corralón es solo el vocabulario.

La vinoteca reutiliza ese dominio casi por completo, pero **renombra el núcleo del
catálogo al lenguaje del negocio**. El cambio real se reduce a:

- **1 entidad nueva:** `GrapeVarietyEntity` (varietal/uva), porque un vino puede ser
  un corte de varias uvas.
- **1 entidad modificada:** `WineEntity` (añade añada, región, graduación, notas de
  cata y la relación con varietales).
- **5 renombres mecánicos:** `ProductEntity → WineEntity`,
  `ProductVariantEntity → WineVariantEntity`, `BrandEntity → WineryEntity`,
  `CategoryEntity → WineTypeEntity`, `SupplierEntity → DistributorEntity`, con sus
  servicios, DTOs, mappers, repositorios, permisos, endpoints y paquetes.
- **Todo lo demás queda intacto:** carrito, órdenes, pagos, stock, direcciones,
  ofertas, usuarios, roles.

Adicionalmente, el pase se aprovecha para sanear la arquitectura: mover reglas de
stock y precio a los lugares correctos, eliminar duplicación y romper dependencias
entre servicios concretos. Esto se hace **sin modelo de dominio paralelo, sin
entidades puras y sin abandonar JPA/Spring**.

La sección 5.8 cataloga los patrones creacionales, estructurales y de comportamiento
aplicables, con el caso de pagos reales como ejemplo de diseño completo.

---

## 3. Estado actual del sistema

> Los nombres de clases de esta sección son los nombres destino. El repositorio de
> partida usa los nombres indicados en la tabla 1.1; la Fase 1 aplica el renombre.

### 3.1 Stack y dependencias

| Componente | Versión / tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | MySQL 8 |
| Seguridad | Spring Security + JWT (jjwt 0.13.0) |
| Hash de contraseñas | BCrypt |
| Mapeo | ModelMapper 3.2.1 (`STRICT`, `skipNullEnabled`) |
| Mail | Spring Mail asíncrono (pool `emailTaskExecutor`) |
| Documentación | SpringDoc OpenAPI 2.8.10 (Swagger UI) |
| Configuración | `spring-dotenv` (`.env`) |
| Utilidades | Lombok |
| Contenedores | Docker + Docker Compose (MySQL + Mailhog + app) |

**Dependencias declaradas en `pom.xml`:** `spring-boot-starter-data-jpa`,
`validation`, `webmvc`, `security`, `mail`, `devtools`, `docker-compose`,
`mysql-connector-j`, `lombok`, `jjwt`, `modelmapper`, `spring-dotenv`,
`springdoc-openapi`. No hay Flyway, ni tests de integración reales, ni caching.

### 3.2 Estructura de paquetes

```
src/main/java/com/utn/vinoteca/
├── VinotecaApplication.java
├── config/
│   ├── AsyncConfig.java          (pool de mails)
│   ├── ModelMapperConfig.java    (ModelMapper STRICT)
│   └── OpenApiConfig.java        (Swagger)
├── exception/
│   ├── BadRequestException.java
│   ├── BusinessRuleException.java
│   ├── EmailAlreadyExistsException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── StockInsufficientException.java
│   └── UnauthorizedException.java
└── features/
    ├── address/        (controller, service, repository, entity, dto, mapper)
    ├── auth/           (seguridad, JWT, roles, permisos, credenciales)
    ├── cart/           (controller, service, repository, entity, dto)
    ├── cart_item/      (entity, dto)
    ├── distributor/    (controller, service, repository, entity, dto, mapper)
    ├── grapeVariety/   (controller, service, repository, entity, dto, mapper)
    ├── notifications/  (controller, service, repository, entity, dto, mapper, enums)
    ├── offer/          (controller, service, repository, entity, dto, mapper)
    ├── offer_product/  (repository, entity, dto, mapper)
    ├── order/          (controller, service, repository, entity, dto, mapper, enums)
    ├── orderItem/      (entity, dto, mapper)
    ├── payment/        (controller, service, repository, entity, dto, mapper, enums)
    ├── stockMovement/  (controller, service, repository, entity, dto, mapper, enums)
    ├── user/           (controller, service, repository, entity, dto, mapper)
    ├── wine/           (controller, service, repository, entity, dto, mapper, specification)
    ├── wineType/       (controller, service, repository, entity, dto, mapper)
    ├── wineVariant/    (controller, service, repository, entity, dto, mapper, specification)
    └── winery/         (controller, service, repository, entity, dto, mapper)
```

**Patrón dominante:** `controller → service (interface + impl) → repository`.
Cada feature tiene su entidad, sus DTOs y su mapper.

### 3.3 Convenciones transversales

| Convención | Implementación actual |
|---|---|
| **DTOs en la frontera** | Los controllers nunca exponen entidades JPA |
| **`externalId` UUID** | Toda entidad expone `externalId` (UUID) además del `id` interno (Long) |
| **Baja lógica** | `active = false` en vino, formato, tipo, bodega, distribuidor, oferta, usuario, dirección |
| **Mapeo** | `ModelMapper` en todos los mappers; algunas asignaciones manuales para relaciones |
| **Servicios** | Interface `IXxxService` + implementación `XxxService(Impl)` |
| **Repositorios** | Spring Data; sin lógica de negocio |
| **Transacciones** | Mezcla de `jakarta.transaction.Transactional` y `org.springframework.transaction.annotation.Transactional` |
| **Errores** | `GlobalExceptionHandler` devuelve `ResponseEntity<String>` (texto plano) |
| **Seguridad por método** | `@PreAuthorize("hasAuthority('...')")` y, en algunos casos, `hasAnyRole('ADMIN','EMPLOYEE')` |
| **Swagger** | `@Operation`, `@ApiResponse`, `@Tag` en todos los controllers |

### 3.4 Modelo de datos completo

#### Entidades y campos

| Entidad | Tabla | Campos principales | Relaciones |
|---|---|---|---|
| `UserEntity` | `users` | `id`, `externalId`, `email` (unique), `name`, `lastName`, `password`, `active`, `createdAt` | `1:N AddressEntity` |
| `CredentialsEntity` | `credentials_entity` | `id`, `username` (unique = email), `password`, `enabled`, `refreshToken` | `1:1 UserEntity`; `N:M RoleEntity` |
| `RoleEntity` | `role_entity` | `id`, `role` (enum, unique) | `N:M PermitEntity` |
| `PermitEntity` | `permit_entity` | `id`, `permit` (enum, unique) | — |
| `AddressEntity` | `addresses` | `id`, `externalId`, `street`, `streetNumber`, `floor`, `apartmentNumber`, `city`, `zipCode`, `active` | `N:1 UserEntity`; `@SQLRestriction("active = true")` |
| `WineTypeEntity` | `wine_types` | `id`, `externalId`, `name`, `active` | `1:N WineEntity` |
| `WineryEntity` | `wineries` | `id`, `externalId`, `name`, `active` | `1:N WineEntity` |
| `DistributorEntity` | `distributors` | `id`, `externalId`, `name`, `contact`, `active` | `1:N WineEntity` |
| `GrapeVarietyEntity` | `grape_varieties` | `id`, `externalId`, `name` (unique), `active` | `N:M WineEntity` |
| `WineEntity` | `wines` | `id`, `externalId`, `name`, `description`, `vintage`, `region`, `alcohol`, `tastingNotes`, `active` | `N:1 DistributorEntity`, `N:1 WineTypeEntity`, `N:1 WineryEntity`; `N:M GrapeVarietyEntity`; `1:N WineVariantEntity` |
| `WineVariantEntity` | `wine_variants` | `id`, `externalId`, `attribute` (formato), `price`, `stock`, `active`, `wholesalePrice`, `wholesaleMinQty` | `N:1 WineEntity`; `1:N StockMovementEntity` |
| `StockMovementEntity` | `stockMovements` | `id`, `externalId`, `type`, `quantity`, `movementDate`, `reason` | `N:1 WineVariantEntity` |
| `CartEntity` | `carts` | `id`, `externalId`, `lastUpdated` | `1:1 UserEntity`; `1:N CartItem` |
| `CartItemEntity` | `cart_items` | `id`, `externalId`, `quantity`, `createdAt` | `N:1 Cart`; `N:1 WineVariantEntity`; unique `(cart_id, wine_variant_id)` |
| `OrderEntity` | `orders` | `id`, `externalId`, `total`, `createdAt`, `status`, `deliveryType` | `N:1 User`; `N:1 Address`; `1:N OrderItem` |
| `OrderItemEntity` | `orderItems` | `id`, `externalId`, `quantity`, `unitPrice`, `subtotal` | `N:1 Order`; `N:1 WineVariantEntity` |
| `PaymentEntity` | `payments` | `id`, `externalId`, `paymentMethod`, `amount`, `paymentStatus`, `createdAt` | `1:1 Order` (unique) |
| `OfferEntity` | `offers` | `id`, `externalId`, `name`, `discountPercentage`, `startDate`, `endDate`, `createdAt`, `active` | `1:N OfferProduct` |
| `OfferProductEntity` | `offer_products` | `id`, `discountedPrice` | `N:1 Offer`; `N:1 WineVariantEntity` |
| `NotificationEntity` | `notifications` | `id`, `externalId`, `message`, `createdAt`, `read`, `type`, `sent` | `N:1 User` |

> `WineImageEntity` se agrega recién en la fase de imágenes (sección 3.10).

#### Enums

| Enum | Valores |
|---|---|
| `Roles` | `ROLE_GUEST`, `ROLE_CUSTOMER`, `ROLE_EMPLOYEE`, `ROLE_ADMIN` |
| `OrderStatus` | `PENDING_PAYMENT`, `PAID`, `CANCELLED` |
| `DeliveryType` | `DELIVERY`, `PICKUP` |
| `PaymentMethod` | `CREDIT_CARD`, `DEBIT_CARD`, `CASH`, `TRANSFER`, `MERCADO_PAGO` |
| `PaymentStatus` | `PENDING`, `APPROVED`, `REJECTED` |
| `StockMovementType` | `ENTRY`, `SALE`, `CANCELLATION`, `ADJUSTMENT` |
| `NotificationType` | `USER_REGISTRATION`, `PASSWORD_CHANGED`, `ORDER_CREATED`, `ORDER_CONFIRMED`, `ORDER_PREPARING`, `ORDER_COMPLETED`, `ORDER_CANCELLED`, `NEW_PROMOTION` |

> `ORDER_CONFIRMED`, `ORDER_PREPARING` y `ORDER_COMPLETED` existen en el enum pero
> no se usan: el flujo real de órdenes solo tiene tres estados.

### 3.5 Seguridad y autorización

- **Autenticación:** `POST /api/auth/login` con `username` (email) y `password`.
  Spring Security valida con `AuthenticationManager`; se emite un JWT de acceso
  (expiración por `JWT_EXPIRATION`).
- **Sesión:** `STATELESS`; el token viaja en `Authorization: Bearer <token>`.
- **Filtro:** `JwtAuthenticationFilter` extrae claims y autoridades.
- **Contraseñas:** BCrypt; nunca se almacenan en texto plano.
- **Autoridades:** `CredentialsEntity.getAuthorities()` devuelve tanto el nombre del
  rol (`ROLE_ADMIN`) como cada permiso (`WINE_CREATE`, etc.).
- **Modelo RBAC:**
  - `CredentialsEntity` (`N:M`) `RoleEntity` (`N:M`) `PermitEntity`.
  - Cada usuario recibe **un rol** en el alta (`UserRequestDTO.role`); el modelo de
    datos soporta varios, pero la API de alta solo asigna uno.
- **Seed:** `SecurityDataInitializer` crea todos los `Permits` y los roles con sus
  permisos al arrancar. No siembra un usuario ADMIN.
- **Endpoints públicos:** solo `/api/auth/**` y Swagger. El catálogo NO es público
  (contradice el rol GUEST definido en las reglas de negocio).

#### Matriz resumida rol → permisos (según seed)

| Rol | Permisos |
|---|---|
| `ROLE_ADMIN` | Todos |
| `ROLE_GUEST` | Lectura y listado de vino, formato, tipo, bodega, varietal y oferta |
| `ROLE_CUSTOMER` | Guest + direcciones + carrito + checkout + órdenes propias + pagos + notificaciones |
| `ROLE_EMPLOYEE` | Alta/modificación/baja de vino, formato, tipo, bodega, varietal y distribuidor; stock y movimientos; ofertas; `ORDER_READ_ALL`, `ORDER_CANCEL_ANY`, `ORDER_MANAGE_STATUS` |

> **Inconsistencia detectada:** `ROLE_EMPLOYEE` no recibe `WINE_READ`,
> `WINE_LIST`, `WINE_VARIANT_READ` ni `WINE_VARIANT_LIST`. Un usuario que sea solo
> EMPLOYEE no puede leer el catálogo por API, aunque pueda modificarlo.

### 3.6 Inventario funcional por módulo

#### Auth

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Registro, login, emisión de JWT |
| Endpoints | `POST /api/auth/register`, `POST /api/auth/login` |
| Reglas | Email único; alta crea `UserEntity` + `CredentialsEntity`; rol por defecto según DTO |
| Archivos | `features/auth/AuthController.java`, `AuthService.java`, `JwtService.java`, `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `UserDetailsServiceImpl.java` |
| Pendiente | Refresh token (campo `refreshToken` sin endpoint), forgot/reset password, admin seed |

#### Users

| Aspecto | Detalle |
|---|---|
| Responsabilidad | CRUD de usuarios con baja lógica |
| Endpoints | `POST /api/users`, `GET /api/users`, `GET /api/users/{externalId}`, `PUT /api/users/{externalId}`, `DELETE /api/users/{externalId}` |
| Reglas | Email único; baja desactiva `UserEntity.active` y `CredentialsEntity.enabled`; cambio de contraseña dispara mail |
| Archivos | `features/user/**` |
| Observación | El update reemplaza el set de roles por uno solo; no permite múltiples roles por API |

#### Addresses

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Direcciones múltiples por usuario |
| Endpoints | `POST /api/addresses`, `GET /api/addresses`, `GET /api/addresses/{id}`, `PUT /api/addresses/{id}`, `DELETE /api/addresses/{id}?userExternalId=`, `GET /api/addresses/user/{userExternalId}` |
| Reglas | Una dirección pertenece a un usuario; baja lógica; `@SQLRestriction` filtra inactivas |
| Archivos | `features/address/**` |
| Deuda | `@RequestMapping("api/addresses")` sin slash inicial |
| Pendiente | Provincia, latitud/longitud, geocoding (Nominatim), autocomplete |

#### Wine Types / Wineries / Distributors

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Catálogos auxiliares del vino (3 módulos gemelos) |
| Endpoints | CRUD + `PATCH /{id}/activate` + `GET /` (activos) + `GET /inactive` |
| Reglas | Nombre obligatorio; baja lógica; solo entidades activas pueden asociarse a vinos |
| Archivos | `features/wineType/**`, `features/winery/**`, `features/distributor/**` |

#### Wines

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Vino comercializable |
| Endpoints | `GET /api/wines/{id}`, `POST /api/wines`, `PUT /api/wines/{id}`, `DELETE /api/wines/{id}`, `PATCH /api/wines/{id}/activate`, `GET /api/wines/search`, `GET /api/wines/inactive` |
| Filtros de búsqueda | `name`, `distributorId`, `wineTypeId`, `wineryId` (+ `grapeVarietyId`, `vintage`, `region` en la fase 2) |
| Reglas | Nombre obligatorio; distribuidor, tipo y bodega activos; no duplicar nombre por bodega; borrar un vino desactiva sus formatos; un vino sin formatos no puede venderse |
| Archivos | `features/wine/**` |
| Deuda | `activate()` tiene la condición invertida (`WineService.java:162`) y nunca reactiva; `PUT` devuelve 201 en vez de 200 (`WineController.java:84`) |

#### Wine Variants (formatos)

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Formato/presentación comercial con stock y precio propios |
| Endpoints | `GET /api/wine-variants/{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`, `PATCH /{id}/activate`, `GET /search`, `GET /inactive`, `POST /stock/entry`, `POST /stock/adjustment`, `GET /{id}/stock` |
| Filtros | `attribute` (formato), `minPrice`, `maxPrice`, `minStock`, `wineId`, `wineTypeId`, `wineryId`, `wineName` |
| Reglas | Formato único por vino; stock propio; precio propio; mayorista opcional (precio menor al minorista y cantidad mínima); stock nunca negativo |
| Archivos | `features/wineVariant/**` |
| Deuda | `GET /{id}/stock` devuelve un `Integer` suelto; el stock se modifica con read-check-write sin locking |

#### Stock Movements

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Trazabilidad de cada cambio de stock |
| Endpoints | `GET /api/stock-movements/variant/{variantId}`, `GET /api/stock-movements/{externalId}` |
| Reglas | Todo cambio de stock genera un movimiento; el servicio es solo de consulta |
| Archivos | `features/stockMovement/**` |
| Deuda | La creación de movimientos está duplicada en tres servicios |

#### Cart

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Carrito persistente por usuario |
| Endpoints | `POST /api/carts`, `GET /api/carts/{userId}`, `PATCH /api/carts/{userId}/items/{variantId}`, `DELETE /api/carts/{userId}`, `POST /api/carts/{userId}/checkout?deliveryType=&addressId=` |
| Reglas | Un carrito por usuario; no reserva stock; valida stock al agregar; el checkout descuenta stock y vacía el carrito |
| Archivos | `features/cart/**`, `features/cart_item/**` |
| Deuda | `CartResponseDTO.userExternalId` nunca se setea (siempre `null`); `calculateUnitPrice` duplicado; depende de la clase concreta `OrderService` |

#### Orders

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Compra confirmada |
| Endpoints | `GET /api/orders/admin`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel?userExternalId=`, `GET /api/orders/user/{userExternalId}`, `GET /api/orders/admin/{id}` |
| Estados | `PENDING_PAYMENT` → `PAID`; `PENDING_PAYMENT` → `CANCELLED`; `PAID` final; `CANCELLED` final |
| Reglas | Total congelado al crear; `DELIVERY` exige dirección del mismo usuario; `PICKUP` no admite dirección; cancelar repone stock y genera movimiento `CANCELLATION` |
| Archivos | `features/order/**`, `features/orderItem/**` |
| Deuda | `ORDER_CANCEL_ANY` y `ORDER_MANAGE_STATUS` sin endpoints; `createFromCart` es un método dios; sin número legible ni historial de estados |

#### Payments

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Pago de una orden |
| Endpoints | `POST /api/payments/pay`, `GET /api/payments/order/{orderId}` |
| Reglas | Solo órdenes `PENDING_PAYMENT`; monto igual al total; aprobado pasa a `PAID`; rechazado cancela y repone stock |
| Archivos | `features/payment/**` |
| Deuda | La pasarela está simulada con `Math.random()`; no hay Strategy; un segundo pago rompe por unique constraint |

#### Offers

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Promociones sobre formatos |
| Endpoints | `POST /api/offers`, `POST /api/offers/{offerId}/products`, `DELETE /api/offers/{offerId}/products/{variantId}`, `GET /api/offers/{id}`, `GET /api/offers`, `GET /api/offers/inactive`, `PATCH /api/offers/{id}/activate`, `PATCH /api/offers/{id}/deactivate` |
| Reglas | Descuento entre 0 y 100; fecha fin posterior a inicio; precio con descuento menor al base; activar envía mail a todos los CUSTOMER; nunca modifica el precio base |
| Archivos | `features/offer/**`, `features/offer_product/**` |
| Deuda | `addProduct` ignora el `offerId` del path y usa el del body; `discountPercentage` y `discountedPrice` conviven con dos fuentes de verdad |

#### Notifications

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Notificaciones in-app |
| Endpoints | `POST /api/notifications`, `GET /api/notifications/user/{userId}`, `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/user/{userId}/read-all`, `DELETE /api/notifications/{id}` |
| Reglas | Ninguna |
| Archivos | `features/notifications/**` |
| Deuda | Ningún evento de negocio crea notificaciones; el flag `sent` nunca pasa a `true`; está huérfana del flujo real |

#### Emails

| Aspecto | Detalle |
|---|---|
| Responsabilidad | Notificaciones por correo (texto plano, asíncronas) |
| Eventos | Bienvenida, contraseña cambiada, orden creada, cambio de estado, cancelación, nueva promoción |
| Implementación | `EmailService` con `@Async("emailTaskExecutor")`, `SimpleMailMessage`, errores silenciosos y logueados |
| Deuda | Se envía dentro de la transacción (puede salir un mail de una orden que luego hace rollback) |

### 3.7 Flujos de negocio clave

#### Checkout (carrito → orden)

1. `CartController.checkout` recibe `userId`, `deliveryType` y `addressId` opcional.
2. `CartServiceImpl.checkout` obtiene el carrito y valida que no esté vacío.
3. `OrderService.createFromCart`:
   - valida el tipo de entrega y la dirección (existencia y pertenencia al usuario);
   - crea la orden;
   - por cada ítem: valida formato activo y stock, calcula precio unitario,
     descuenta stock, genera movimiento `SALE` y crea el `OrderItem`;
   - calcula y congela el total;
   - persiste la orden;
   - envía el mail de orden creada (dentro de la transacción).
4. `CartServiceImpl` vacía el carrito.

#### Pago

1. `POST /api/payments/pay` con `orderId`, `paymentMethod`, `amount`.
2. Valida que la orden esté `PENDING_PAYMENT` y que el monto coincida.
3. Crea el pago en `PENDING` y simula la tarjeta.
4. Si aprueba: pago `APPROVED`, orden `PAID`, mail de cambio de estado.
5. Si rechaza: pago `REJECTED`, orden `CANCELLED`, repone stock, movimiento
   `CANCELLATION`, mail de cancelación.

#### Cancelación

1. CUSTOMER solo puede cancelar sus propias órdenes; EMPLOYEE/ADMIN cualquier orden
   (la lógica existe en el servicio, pero no hay endpoint con `ORDER_CANCEL_ANY`).
2. No se puede cancelar una orden `PAID` ni una ya `CANCELLED`.
3. Repone stock por ítem y genera un movimiento `CANCELLATION` por cada uno.

#### Precio unitario

Cadena actual (duplicada en `CartServiceImpl` y `OrderService`):

```
oferta activa (OfferProduct.discountedPrice)
  → si no hay: precio mayorista (si cantidad ≥ wholesaleMinQty)
  → si no: precio minorista (WineVariant.price)
```

### 3.8 Reglas de negocio e invariantes

| # | Invariante | Estado en código |
|---|---|---|
| 1 | No existe stock negativo | Parcial: se valida en checkout y ajuste, pero no hay locking; concurrencia puede violarlo |
| 2 | No existen emails duplicados | Cumplido (`existsByEmail` + unique) |
| 3 | No existen órdenes sin usuario | Cumplido (`nullable = false`) |
| 4 | No existen formatos sin vino | Cumplido (`nullable = false`) |
| 5 | No existen carritos sin usuario | Cumplido (`nullable = false`, unique) |
| 6 | Toda modificación de stock genera un movimiento | Cumplido, pero con lógica duplicada |
| 7 | No puede comprarse más stock del disponible | Parcial: sin locking |
| 8 | Toda orden pertenece a un usuario | Cumplido |
| 9 | Todo usuario posee al menos un rol | Cumplido en el alta (un rol) |
| 10 | Un vino inactivo no puede venderse | Parcial: el checkout valida formato activo, no vino activo |

### 3.9 Deuda técnica y bugs conocidos

| # | Tipo | Problema | Ubicación (post-renombre) |
|---|---|---|---|
| 1 | Bug | `activate()` con condición invertida: nunca reactiva | `features/wine/service/WineService.java:162` |
| 2 | Bug | `addProduct` ignora el `offerId` del path y usa el del body | `features/offer/controller/OfferController.java:64-67` |
| 3 | Bug | `PUT` de vino devuelve 201 | `features/wine/controller/WineController.java:84` |
| 4 | Bug | `CartResponseDTO.userExternalId` siempre `null` | `features/cart/service/CartServiceImpl.java:187` |
| 5 | Bug | Cancelación/gestión por EMPLOYEE sin endpoint | `features/order/controller/OrderController.java` |
| 6 | Deuda | Refresh token y `RefreshTokenRequest` sin uso | `features/auth/**` |
| 7 | Bug | `@RequestMapping("api/addresses")` sin slash | `features/address/controller/AddressController.java:25` |
| 8 | Deuda | Notificaciones in-app desconectadas de eventos | `features/notifications/**` |
| 9 | Deuda | `System.out.println` en autenticación y seed | `CredentialsEntity.java:54`, `SecurityDataInitializer.java:150` |
| 10 | Deuda | `calculateUnitPrice` duplicado | `CartServiceImpl.java:229`, `OrderService.java:294` |
| 11 | Deuda | `restoreStock` y `createStockMovement` triplicados | `OrderService`, `PaymentService`, `WineVariantService` |
| 12 | Deuda | `@Transactional` mezcla Jakarta y Spring | `OrderService.java:32`, `WineService.java:20`, `WineVariantService.java:17` |
| 13 | Deuda | Errores como texto plano | `exception/GlobalExceptionHandler.java` |
| 14 | Deuda | Falta paginación en todos los listados | Todos los controllers |
| 15 | Deuda | Catálogo no público (contradice GUEST) | `SecurityConfig.java:42-45` |
| 16 | Deuda | Sin tests (solo `contextLoads`) | `src/test` |
| 17 | Deuda | N+1 por relaciones EAGER y búsquedas por credenciales | `UserService.getAll`, `OfferService.activate`, `WineEntity` |
| 18 | Deuda | Código muerto (`OrderRequestDTO`, mappers, imports) | Varios |

### 3.10 Funcionalidades proyectadas (`PLAN_DE_MEJORAS.md`)

**Bugs a corregir (prioridad alta):** los 9 puntos de la tabla 3.9.

**Requerimientos solicitados:**

- Paginación con `PageResponse<T>` propio (no exponer `Page` de Spring Data),
  `size` máximo 100 y default 20.
- Enriquecer `PaymentResponseDTO` (incluir estado de orden) y envolver el stock
  suelto en un objeto.
- Geocoding con OpenStreetMap/Nominatim: provincia, latitud, longitud,
  `displayName`, autocomplete y degradación elegante si el servicio falla.
- Imágenes con Cloudinary: entidad `WineImageEntity` (o campos en vino), upload
  firmado desde backend, transformaciones (`f_auto,q_auto,w_400`, etc.).

**Mejoras por feature:**

- Notificaciones event-driven con `ApplicationEventPublisher` +
  `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`.
- Órdenes: máquina de estados explícita, número legible (`ORD-2026-000123`),
  idempotencia en checkout, historial de estados, cancelación por EMPLOYEE/ADMIN.
- Pagos: `PaymentGateway` (Strategy) con `SimulatedCardGateway`,
  `MercadoPagoGateway`, `TransferGateway`; guard de pago duplicado; idempotencia.
- Stock: locking optimista (`@Version`) o update atómico con guard;
  `StockService` único; opcional stock reservado con timeout.
- Precios: `PricingService` único; resolver `discountPercentage` vs
  `discountedPrice`.
- Seguridad: catálogo público, un solo modelo de autorización, refresh real,
  forgot/reset password, admin seed, eliminar `System.out.println`.
- Ofertas: corregir `@PathVariable`, activación automática por fechas.

**Transversales:** unificar `@Transactional`, `ErrorResponse` unificado, Flyway,
tests de invariantes, arreglar N+1, limpiar código muerto.

**Extras:** MercadoPago, costo de envío por zona, observabilidad (Actuator),
cache de catálogo, rate limiting y versionado `/api/v1`, auditoría, import CSV.

### 3.11 Configuración y entorno

**`src/main/resources/application.yaml`:**

- `spring.datasource`: MySQL configurable por `DB_HOST`, `DB_PORT`, `DB_NAME`,
  `DB_USER`, `DB_PASSWORD`.
- `spring.jpa.hibernate.ddl-auto: update` (sin Flyway).
- `spring.mail`: SMTP configurable por `MAIL_*` (Mailhog en desarrollo).
- `jwt.secret`, `jwt.expiration`.
- `server.port: 8081`.
- Swagger en `/swagger-ui.html` y `/v3/api-docs`.
- `app.base-url` para los links de los mails.

**`compose.yaml`:** MySQL 8 (puerto host 3307), Mailhog (1025/8025) y la app
(8080→8081) con healthcheck y red `vinoteca-network`.

**`Dockerfile`:** build multi-stage con Maven + Temurin 21.

**Variables de entorno:** `DB_*`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`,
`JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `MAIL_*`, `APP_BASE_URL`.
El archivo `.env` está en `.gitignore` y no debe subirse.

---

## 4. Dominio vinoteca

### 4.1 Conceptos del negocio

| Concepto | Definición | Dónde vive |
|---|---|---|
| **Vino** | Etiqueta comercial (ej. "Malbec Reserva") | `WineEntity` |
| **Bodega** | Empresa productora (ej. "Zuccardi") | `WineryEntity` |
| **Tipo** | Tinto, blanco, rosado, espumante, dulce | `WineTypeEntity` |
| **Varietal / Uva** | Malbec, Cabernet Sauvignon, Chardonnay, etc. | `GrapeVarietyEntity` |
| **Corte / Blend** | Vino con más de un varietal | Relación N:M vino–varietal |
| **Añada** | Año de cosecha | `WineEntity.vintage` |
| **Región** | Valle de Uco, Luján de Cuyo, Patagonia, etc. | `WineEntity.region` |
| **Formato / Presentación** | Botella 750 ml, magnum 1500 ml, caja x6 | `WineVariantEntity.attribute` |
| **Precio minorista** | Precio de góndola | `WineVariantEntity.price` |
| **Precio mayorista** | Precio por volumen para restaurantes/revendedores | `wholesalePrice` + `wholesaleMinQty` |
| **Stock** | Unidades disponibles por formato | `WineVariantEntity.stock` |
| **Distribuidor** | Empresa que abastece al comercio | `DistributorEntity` |

### 4.2 Mapeo corralón → vinoteca

| Corralón | Vinoteca | Cambio |
|---|---|---|
| `ProductEntity` (Cemento Portland) | `WineEntity` (Malbec Reserva) | Renombre + campos de vino + N:M varietales |
| `ProductVariantEntity` (Bolsa 50 kg) | `WineVariantEntity` (Botella 750 ml) | Renombre |
| `BrandEntity` (Loma Negra) | `WineryEntity` (Zuccardi) | Renombre |
| `CategoryEntity` (Cementos) | `WineTypeEntity` (Tintos) | Renombre |
| `SupplierEntity` (Proveedores S.A.) | `DistributorEntity` (Distribuidora del Oeste) | Renombre |
| `price` / `stock` / `wholesalePrice` / `wholesaleMinQty` | Idénticos | Ninguno |
| `Cart` / `CartItem` | Idénticos | Ninguno |
| `Order` / `OrderItem` | Idénticos | Ninguno |
| `Payment` | Idénticos | Ninguno |
| `Address` | Idénticos (envío a domicilio o retiro) | Ninguno |
| `Offer` / `OfferProduct` | Idénticos (2x1, descuentos por caja) | Ninguno |
| `StockMovement` | Idénticos | Ninguno |
| `User` / `Credentials` / `Role` / `Permit` | Idénticos | Ninguno |
| — | **`GrapeVarietyEntity`** | Entidad nueva |
| — | Añada, región, graduación, notas de cata | Campos nuevos en vino |

**Conclusión:** el único concepto realmente nuevo es el **varietal**, y solo porque
un vino puede ser un corte. Todo lo demás es renombre de vocabulario.

### 4.3 Alcance del cambio

#### Se renombra (mecánico)

- `ProductEntity` → `WineEntity` y todo su módulo (`WineService`, `WineController`,
  `WineRepository`, `WineMapper`, `WineSpecification`, DTOs).
- `ProductVariantEntity` → `WineVariantEntity` y todo su módulo.
- `BrandEntity` → `WineryEntity` y todo su módulo.
- `CategoryEntity` → `WineTypeEntity` y todo su módulo.
- `SupplierEntity` → `DistributorEntity` y todo su módulo.
- Paquetes `features/product`, `features/productVariant`, `features/brand`,
  `features/category`, `features/supplier`.
- Permisos `PRODUCT_*`, `PRODUCT_VARIANT_*`, `BRAND_*`, `CATEGORY_*`,
  `SUPPLIER_*`.
- Endpoints `/api/products`, `/api/product-variants`, `/api/brands`,
  `/api/categories`, `/api/suppliers`.
- Tablas `products`, `productVariants`, `brands`, `categories`, `suppliers`.

#### Se modifica (lógica)

- `WineEntity`: campos de vino + relación con varietales.
- `WineRequestDTO` / `WineResponseDTO`.
- `WineMapper`.
- `WineSpecification` (filtros nuevos).
- `WineService` / `IWineService` (búsqueda por varietal, añada, región).
- `WineController` (parámetros de búsqueda nuevos).
- Seed de datos (varietales, tipos, bodegas de ejemplo).
- Documentación y textos de identidad del sitio (mails, Swagger, README).

#### Se crea

- `GrapeVarietyEntity` (`id`, `externalId`, `name`, `active`).
- `GrapeVarietyRepository`, `GrapeVarietyService`, `GrapeVarietyController`,
  DTOs y mapper (módulo gemelo de WineType/Winery).
- Relación N:M `wine_grape_varieties`.

#### No se modifica

- `Cart`, `CartItem`, `Order`, `OrderItem`, `Payment`, `Address`, `Offer`,
  `OfferProduct`, `StockMovement`, `Notification`, `User`, `Credentials`,
  `Role`, `Permit`, sus servicios, DTOs y controllers (salvo correcciones del
  plan de mejoras).

### 4.4 Cambios de modelo de datos

#### Nueva entidad `GrapeVarietyEntity`

```java
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "grape_varieties")
public class GrapeVarietyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId", nullable = false, unique = true, updatable = false)
    @UuidGenerator
    private UUID externalId = UUID.randomUUID();

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
```

> El nombre es único porque "Malbec" debe existir una sola vez en el sistema.

#### Modificación de `WineEntity`

```java
@Column(name = "vintage")
private Integer vintage;                 // añada, ej. 2021

@Column(name = "region", length = 120)
private String region;                   // ej. "Valle de Uco"

@Column(name = "alcohol", precision = 4, scale = 1)
private BigDecimal alcohol;              // ej. 13.5

@Column(name = "tasting_notes", length = 1000)
private String tastingNotes;             // notas de cata

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "wine_grape_varieties",
        joinColumns = @JoinColumn(name = "wine_id"),
        inverseJoinColumns = @JoinColumn(name = "grape_variety_id")
)
private Set<GrapeVarietyEntity> grapeVarieties = new HashSet<>();
```

> `vintage`, `region`, `alcohol` y `tastingNotes` son opcionales para no romper
> vinos existentes ni el alta mínima.

#### Justificación del N:M

- Un varietal puro (Malbec) tiene un solo registro asociado.
- Un corte (Malbec + Cabernet + Merlot) tiene varios.
- Un campo `String grape` no permitiría filtrar cortes por uva ni evitar
  duplicados.

### 4.5 Cambios de API, DTOs y filtros

#### `WineRequestDTO` (nuevos campos)

```java
private Integer vintage;
private String region;
private BigDecimal alcohol;
private String tastingNotes;
private Set<UUID> grapeVarietyIds;   // varietales asociados
```

#### `WineResponseDTO` (nuevos campos)

```java
private Integer vintage;
private String region;
private BigDecimal alcohol;
private String tastingNotes;
private List<GrapeVarietySummaryDTO> grapeVarieties;
```

#### Filtros de búsqueda de vino

`GET /api/wines/search` agrega:

| Parámetro | Tipo | Semántica |
|---|---|---|
| `grapeVarietyId` | UUID | Varietal específico |
| `vintage` | Integer | Añada exacta |
| `region` | String | Texto parcial (case-insensitive) |
| `minPrice` / `maxPrice` | BigDecimal | Rango de precio (a nivel formato) |
| `inStock` | Boolean | Solo formatos con stock > 0 |

Los filtros de **bodega** (`wineryId`) y **tipo** (`wineTypeId`) ya existen (como
`brandId` y `categoryId`) y se reutilizan sin cambios de lógica.

> Decisión de diseño: el filtro por precio y stock vive naturalmente en el formato,
> no en el vino. Si se expone en `/api/wines/search`, se debe implementar con un
> `join` a formatos o delegar en `/api/wine-variants/search`. Se documenta la opción
> elegida en el PR.

#### Endpoints del módulo varietal

| Método | Ruta | Permiso |
|---|---|---|
| `POST` | `/api/grape-varieties` | `GRAPE_VARIETY_CREATE` |
| `GET` | `/api/grape-varieties` | `GRAPE_VARIETY_LIST` |
| `GET` | `/api/grape-varieties/{externalId}` | `GRAPE_VARIETY_READ` |
| `PUT` | `/api/grape-varieties/{externalId}` | `GRAPE_VARIETY_UPDATE` |
| `DELETE` | `/api/grape-varieties/{externalId}` | `GRAPE_VARIETY_DELETE` |
| `PATCH` | `/api/grape-varieties/{externalId}/activate` | `GRAPE_VARIETY_ACTIVATE` |
| `GET` | `/api/grape-varieties/inactive` | `GRAPE_VARIETY_LIST_INACTIVE` |

Agregar los permisos a `Permits`, al seed de ADMIN, al de GUEST/CUSTOMER (solo
lectura y listado) y al de EMPLOYEE (gestión completa).

### 4.6 Datos semilla

Se recomienda un `WineDataInitializer` idempotente (mismo patrón que
`SecurityDataInitializer`) con:

- **Tipos** (`WineTypeEntity`): Tinto, Blanco, Rosado, Espumante, Dulce.
- **Varietales** (`GrapeVarietyEntity`): Malbec, Cabernet Sauvignon, Merlot,
  Syrah, Bonarda, Tempranillo, Chardonnay, Sauvignon Blanc, Torrontés, Pinot Noir.
- **Bodegas** (`WineryEntity`): 3–5 de ejemplo.
- **Distribuidores** (`DistributorEntity`): 1–2 de ejemplo.
- **Vinos** (`WineEntity`) + **formatos** (`WineVariantEntity`): 5–10 de ejemplo con
  stock, precio minorista y mayorista.

> El seed debe poder ejecutarse varias veces sin duplicar datos (chequear por
> nombre/externalId antes de insertar).

### 4.7 Reglas de negocio específicas

Además de los invariantes existentes, la vinoteca puede sumar:

| Regla | Implementación sugerida |
|---|---|
| Un vino debe tener al menos un varietal | Validación en alta/edición |
| La añada debe estar entre 1900 y el año actual + 1 | Validación en DTO/servicio |
| La graduación alcohólica debe estar entre 0 y 100 | Validación en DTO |
| No se puede duplicar el mismo vino + añada + bodega | Unique lógico en servicio |
| Un formato no puede repetirse dentro del mismo vino | Ya existe (`existsByWineAndAttribute`) |

---

## 5. Arquitectura objetivo

### 5.1 Principios

1. **Un solo modelo.** Las entidades siguen siendo JPA. No hay modelo de dominio
   paralelo ni entidades puras.
2. **Spring en todas las capas.** Servicios `@Service`, repositorios Spring Data,
   transacciones de Spring.
3. **Responsabilidad única por clase.** El objetivo del pase es que cada clase
   tenga una razón clara para cambiar.
4. **Dependencias hacia adentro.** `Controller → Service → (Entity/Reglas +
   Repository)`. Nunca al revés, nunca salteando capas.
5. **Puertos solo donde aportan.** Interfaces donde hay múltiples
   implementaciones (pago) o donde se aísla un externo (mapa, imágenes, mail).
6. **Cambios pequeños y verificables.** Cada extracción con test antes y después.

### 5.2 Estructura de carpetas

Se mantiene el monolito modular por features. Dentro de cada feature:

```
features/<feature>/
├── controller/     Entrada HTTP. Sin lógica de negocio.
├── dto/            Contratos de entrada y salida.
├── mapper/         Traducción entity <-> dto.
├── entity/         JPA + comportamiento de negocio propio.
├── service/        Casos de uso y orquestación (@Service, @Transactional).
└── repository/     Acceso a datos (Spring Data).
```

Features transversales de alta cohesión:

```
features/pricing/   PricingService: única fuente de verdad del precio.
features/stock/     StockService: descuento/reposición + movimiento.
```

> No se crean carpetas `domain/` ni `application/` genéricas vacías. La separación
> es por responsabilidad real, no por moda.

### 5.3 Delegación de responsabilidades

| Responsabilidad | Quién | Patrón GRASP |
|---|---|---|
| Recibir HTTP, validar formato, responder | `controller` | Controller |
| Orquestar caso de uso y transacción | `service` | Indirection / Pure Fabrication |
| Reglas que le pertenecen a un dato | **la entidad** | Information Expert |
| Reglas que cruzan entidades | domain service `@Component` | Pure Fabrication |
| Crear agregados complejos | `factory` | Creator |
| Traducir entity ↔ dto | `mapper` | Pure Fabrication |
| Persistir | `repository` | Adaptador de datos |
| Variar un algoritmo o externo | interfaz + implementaciones | Polymorphism / Protected Variations |

### 5.4 SOLID aplicado

| Principio | Situación actual | Objetivo | Patrón que lo materializa |
|---|---|---|---|
| **SRP** | `OrderService.createFromCart` valida, resuelve dirección, crea orden, descuenta stock, genera movimientos, calcula precio y envía mail | `CheckoutService` orquesta; `PricingService` calcula; `StockService` mueve stock; el mail sale por evento | Facade / Mediator + Observer |
| **OCP** | `simulateCard()` hardcodeado | `PaymentGateway` con implementaciones intercambiables | Strategy + Factory Method + Registry |
| **LSP** | `CartServiceImpl` depende de la clase concreta `OrderService` | Depende de `ICheckoutService` | Strategy / Adapter (programar contra la interfaz) |
| **ISP** | Interfaces de servicio grandes | Contratos por caso de uso cuando haga falta | Ports (interfaces pequeñas por caso de uso) |
| **DIP** | Acoplamiento a clases concretas entre servicios | Depender de interfaces; los repos ya cumplen | Adapter + Ports |
| **DRY** | `calculateUnitPrice` x2 y `restoreStock` x3 | Una sola implementación en `PricingService` y `StockService` | Pure Fabrication |

### 5.5 GRASP aplicado

| Patrón GRASP | Dónde se aplica | Patrón de diseño que lo materializa |
|---|---|---|
| **Information Expert** | `WineVariantEntity.decrementStock/restoreStock`; `PricingService` | Rich Domain Model (métodos en la entidad) |
| **Creator** | `OrderFactory.createFrom(cart, address, deliveryType)`; `StockMovementFactory` | Factory Method / Builder |
| **Controller** | Controllers HTTP finos | Front Controller (Spring MVC) |
| **Low Coupling** | Cart deja de conocer Order; ambos conocen interfaces | Observer / Facade / Mediator |
| **High Cohesion** | `PricingService` y `StockService` agrupan una sola responsabilidad | Pure Fabrication |
| **Polymorphism** | `PaymentGateway` | Strategy |
| **Pure Fabrication** | `PricingService`, `StockService`, mappers, factories | Service / Factory |
| **Indirection** | `ICheckoutService` entre cart y order | Facade / Adapter |
| **Protected Variations** | `PaymentGateway`; interfaces para mapa, imágenes y mail | Adapter / Strategy / Decorator / Proxy |

### 5.6 Extracciones concretas (antes/después)

#### 5.6.1 Stock en la entidad (Information Expert)

**Antes** (`OrderService.java:138`):

```java
wineVariant.setStock(wineVariant.getStock() - cartItem.getQuantity());
```

**Después** (en `WineVariantEntity`):

```java
public void decrementStock(int quantity) {
    if (quantity <= 0) {
        throw new BusinessRuleException("Quantity must be positive");
    }
    if (this.stock < quantity) {
        throw new StockInsufficientException(this.externalId.toString());
    }
    this.stock -= quantity;
}

public void restoreStock(int quantity) {
    if (quantity <= 0) {
        throw new BusinessRuleException("Quantity must be positive");
    }
    this.stock += quantity;
}
```

El invariante "no hay stock negativo" queda en el único lugar que posee el dato.
La entidad sigue siendo JPA.

#### 5.6.2 `PricingService` (elimina duplicación)

**Antes:** `calculateUnitPrice` en `CartServiceImpl.java:229` y
`OrderService.java:294`.

**Después:**

```java
@Service
@RequiredArgsConstructor
public class PricingService {

    private final OfferProductRepository offerProductRepository;

    public BigDecimal calculateUnitPrice(WineVariantEntity variant, int quantity) {
        return offerProductRepository.findActiveOfferForWineVariant(variant.getExternalId())
                .map(OfferProductEntity::getDiscountedPrice)
                .orElseGet(() -> calculateWithoutOffer(variant, quantity));
    }

    private BigDecimal calculateWithoutOffer(WineVariantEntity variant, int quantity) {
        boolean wholesaleApplies =
                variant.getWholesaleMinQty() != null
                        && variant.getWholesalePrice() != null
                        && quantity >= variant.getWholesaleMinQty();

        return wholesaleApplies
                ? variant.getWholesalePrice()
                : variant.getPrice();
    }
}
```

#### 5.6.3 `StockService` (elimina triplicación)

**Antes:** `restoreStock`/`createStockMovement` en `OrderService`,
`PaymentService` y `WineVariantService`.

**Después:**

```java
@Service
@RequiredArgsConstructor
public class StockService {

    private final WineVariantRepository variantRepository;
    private final StockMovementRepository movementRepository;

    @Transactional
    public void discount(WineVariantEntity variant, int quantity, String reason) {
        variant.decrementStock(quantity);
        variantRepository.save(variant);
        registerMovement(variant, quantity, StockMovementType.SALE, reason);
    }

    @Transactional
    public void restore(WineVariantEntity variant, int quantity, String reason) {
        variant.restoreStock(quantity);
        variantRepository.save(variant);
        registerMovement(variant, quantity, StockMovementType.CANCELLATION, reason);
    }

    @Transactional
    public void registerEntry(WineVariantEntity variant, int quantity, String reason) {
        variant.restoreStock(quantity);
        variantRepository.save(variant);
        registerMovement(variant, quantity, StockMovementType.ENTRY, reason);
    }

    @Transactional
    public void adjust(WineVariantEntity variant, int delta, String reason) {
        if (variant.getStock() + delta < 0) {
            throw new BusinessRuleException("Stock cannot be negative");
        }
        variant.setStock(variant.getStock() + delta);
        variantRepository.save(variant);
        registerMovement(variant, delta, StockMovementType.ADJUSTMENT, reason);
    }

    private void registerMovement(WineVariantEntity variant, int quantity,
                                  StockMovementType type, String reason) {
        movementRepository.save(StockMovementEntity.builder()
                .movementDate(LocalDateTime.now())
                .quantity(quantity)
                .type(type)
                .reason(reason)
                .wineVariant(variant)
                .build());
    }
}
```

#### 5.6.4 `OrderStatusPolicy` (transiciones centralizadas)

```java
@Component
public class OrderStatusPolicy {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    public void assertTransition(OrderStatus from, OrderStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessRuleException(
                    "Invalid order status transition: " + from + " -> " + to);
        }
    }
}
```

#### 5.6.5 `PaymentGateway` (Strategy)

```java
public interface PaymentGateway {
    PaymentResult process(PaymentRequestDTO request, OrderEntity order);
}
```

Implementaciones:

- `SimulatedCardGateway` (comportamiento actual, para desarrollo).
- `TransferGateway` (aprobación manual/pendiente).
- `MercadoPagoGateway` (fase extra).

`PaymentService` selecciona la implementación según `PaymentMethod`. Spring inyecta
un `Map<PaymentMethod, PaymentGateway>`.

El diseño completo para pasarelas reales (Adapter, Template Method, Decorator,
State, Command, Observer y webhooks) está en la sección 5.8.6.

#### 5.6.6 `CheckoutService` y `ICheckoutService`

- `CheckoutService` recibe el carrito, valida, crea la orden, delega precio y stock.
- `CartServiceImpl` depende de `ICheckoutService`, no de `OrderService`.

### 5.7 Límites explícitos

- **No** se crea un modelo de dominio separado.
- **No** se convierten las entidades en POJOs sin JPA.
- **No** se mueven las entidades a otra capa ni se cambia la persistencia.
- **No** se introducen puertos/adapters por moda.
- **No** se parte en microservicios.
- **No** se cambia el contrato HTTP sin documentarlo.

### 5.8 Catálogo de patrones de diseño

Esta sección enumera los patrones que se pueden aplicar, en qué situación y con qué
costo. No es una lista para implementar toda: es un catálogo de decisión.

#### 5.8.1 Criterio de aplicación

Un patrón entra solo si cumple al menos una de estas condiciones:

1. **Variación real:** hay dos o más implementaciones, algoritmos o proveedores.
2. **Preocupación transversal:** retry, cache, logs, métricas, idempotencia o
   seguridad.
3. **Límite con un externo:** SDK de pago, API de mapas, storage de imágenes o SMTP.
4. **Estructura compuesta:** reglas, categorías, canales o pasos que se combinan.

Si no cumple ninguna, no se agrega. La indirección sin variación es deuda, no diseño.

#### 5.8.2 Patrones ya presentes

| Patrón | Dónde ya existe |
|---|---|
| Singleton | `@Service`, `@Component`, `@Repository` de Spring |
| Builder | Lombok `@Builder` en entidades y DTOs |
| Repository | Spring Data JPA |
| Specification | `WineSpecification`, `WineVariantSpecification` |
| DTO / Mapper | Toda la frontera HTTP |
| Front Controller / MVC | Spring MVC |
| Proxy | Lazy loading de Hibernate y AOP de Spring Security |
| Dependency Injection | Todo el proyecto |

#### 5.8.3 Creacionales

| Patrón | Dónde | Situación | Por qué |
|---|---|---|---|
| **Factory Method** | `PaymentGatewayFactory` / `PaymentGatewayRegistry` | Elegir la pasarela según `PaymentMethod` | OCP + Creator |
| **Abstract Factory** | `PaymentProviderFactory` | Familia MercadoPago: gateway + parser de webhook + validador de firma + reembolso | Solo si el proveedor exige una familia cohesiva |
| **Builder** | `OrderResponseBuilder`, `PaymentResultBuilder` | Armar respuestas con muchos campos opcionales | Legibilidad |
| **Prototype** | `RepeatOrderService`, `PromotionCloner` | "Repetir pedido", duplicar una promoción o un vino con sus formatos | Evita reconstruir agregados |
| **Singleton** | Spring | Servicios sin estado | Ya lo da el contenedor; no implementar a mano |
| **Object Pool** | No aplicar | — | HikariCP ya lo resuelve |

> Abstract Factory no se usa con un solo proveedor. Se empieza con Factory +
> Registry y se promueve cuando aparece el segundo proveedor que necesite familia.

#### 5.8.4 Estructurales

| Patrón | Dónde | Situación | Por qué |
|---|---|---|---|
| **Adapter** | `MercadoPagoAdapter`, `NominatimAdapter`, `CloudinaryAdapter` | SDK externo → puerto propio | DIP + Protected Variations |
| **Facade** | `CheckoutFacade`, `CatalogFacade` | Ocultar subsistema (cart + stock + pricing + order + payment) | SRP + Low Coupling |
| **Decorator** | `RetryPaymentGateway`, `CachedGeocodingAdapter`, `LoggingEmailSender` | Retry, cache, logs, métricas, idempotencia | OCP + composición |
| **Proxy** | `CachedCatalogProxy`, `RateLimitedNominatimProxy` | Cache, rate limit, lazy, seguridad | Protected Variations |
| **Composite** | `DiscountComposite`, `WineTypeTree` | Reglas de descuento combinables; árbol de tipos | Hoja y compuesto se tratan igual |
| **Bridge** | `NotificationChannel` (Email/SMS/Push) | Separar "qué notifico" de "por qué canal" | Evita explosión de subclases |
| **Flyweight** | Solo referencia inmutable | Varietales/tipos compartidos | Bajo valor con JPA; preferir cache |
| **Registry** | `PaymentGatewayRegistry` | Descubrir estrategias por clave | OCP |

> **Adapter vs Facade:** el Adapter traduce un externo a tu contrato; el Facade
> simplifica un subsistema interno. En pagos se necesitan ambos.

> **Decorator vs Proxy:** el Decorator agrega comportamiento y se compone
> explícitamente; el Proxy controla el acceso y suele ser transparente (AOP, lazy,
> cache).

#### 5.8.5 De comportamiento

| Patrón | Dónde | Situación |
|---|---|---|
| **Strategy** | `PricingStrategy`, `ShippingCostStrategy`, `DiscountStrategy` | Oferta vs mayorista vs minorista; costo por zona |
| **State** | `OrderState`, `PaymentState` | `PENDING_PAYMENT → PAID/CANCELLED`; `PENDING → APPROVED/REJECTED/REFUNDED` |
| **Template Method** | `AbstractPaymentGateway`, `AbstractCheckoutStep` | Esqueleto fijo con pasos variables por proveedor |
| **Observer** | Eventos de dominio | `OrderCreated`, `OrderPaid`, `PaymentApproved` |
| **Chain of Responsibility** | `CheckoutValidationChain`, `DiscountRuleChain` | Validaciones y reglas en cadena |
| **Command** | `ChargePaymentCommand`, `CancelOrderCommand`, `AdjustStockCommand` | Operación + idempotencia + auditoría + undo |
| **Mediator** | `CheckoutMediator` | Coordinar cart/stock/pricing/order/payment sin acoplarlos |
| **Visitor** | `OrderCsvExporter`, `WineReportExporter` | Muchas exportaciones sobre una estructura estable |
| **Null Object** | `NoOpNotificationChannel` | Evitar `null` checks en canales deshabilitados |
| **Memento** | `OrderStatusHistory` | Snapshot de estado para auditoría |

> **Strategy vs State:** Strategy elige un algoritmo en runtime; State modela
> transiciones y comportamiento que cambia según el estado. Para órdenes y pagos,
> State. Para precios y envío, Strategy.

> **Composite vs Chain:** Composite combina resultados (sumar descuentos); Chain se
> detiene cuando un eslabón resuelve. Para promociones se pueden usar los dos: Chain
> para elegir la regla aplicable, Composite para combinar descuentos.

#### 5.8.6 Caso de estudio: pagos reales

Hoy `PaymentService.pay()` hace todo y `simulateCard()` decide con `Math.random()`.
Con pasarelas reales el diseño se separa en capas:

```text
PaymentController
   └── PaymentService (Facade, orquesta, @Transactional)
         ├── PaymentGatewayRegistry  (Factory/Registry por PaymentMethod)
         │     └── PaymentGateway (Strategy)
         │           ├── LoggingPaymentGateway      (Decorator)
         │           ├── RetryPaymentGateway        (Decorator)
         │           ├── IdempotentPaymentGateway   (Decorator + Command)
         │           └── MercadoPagoAdapter         (Adapter del SDK)
         ├── PaymentStatePolicy  (State)
         ├── PaymentRepository
         └── ApplicationEventPublisher (Observer)
```

**Puerto y estrategia**

```java
public interface PaymentGateway {
    PaymentMethod method();
    PaymentResult charge(PaymentCommand command);
    RefundResult refund(RefundCommand command);
}
```

```java
public record PaymentCommand(
        UUID orderId,
        BigDecimal amount,
        String idempotencyKey,
        Map<String, Object> providerData
) {}
```

**Factory + Registry**

```java
@Component
public class PaymentGatewayRegistry {

    private final Map<PaymentMethod, PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.gateways = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::method, Function.identity()));
    }

    public PaymentGateway forMethod(PaymentMethod method) {
        return Optional.ofNullable(gateways.get(method))
                .orElseThrow(() -> new BusinessRuleException(
                        "Unsupported payment method: " + method));
    }
}
```

**Template Method**

```java
public abstract class AbstractPaymentGateway implements PaymentGateway {

    @Override
    public final PaymentResult charge(PaymentCommand command) {
        validate(command);
        PaymentResult result = doCharge(command);
        return mapResult(result);
    }

    protected abstract PaymentResult doCharge(PaymentCommand command);
    protected abstract PaymentResult mapResult(Object providerResult);
    protected void validate(PaymentCommand command) { /* común */ }
}
```

**Decorators**

```java
public class RetryPaymentGateway implements PaymentGateway { /* backoff */ }
public class LoggingPaymentGateway implements PaymentGateway { /* logs + métricas */ }
public class IdempotentPaymentGateway implements PaymentGateway { /* clave idempotente */ }
```

Composición:

```java
new LoggingPaymentGateway(
    new RetryPaymentGateway(
        new IdempotentPaymentGateway(
            new MercadoPagoAdapter(mercadoPagoClient))));
```

**State**

```java
public enum PaymentStatus { PENDING, APPROVED, REJECTED, REFUNDED }
```

`PaymentStatePolicy` centraliza las transiciones válidas. Nada de `if (status == ...)`
disperso por el código.

**Observer**

`PaymentService` publica `PaymentApprovedEvent`. Un listener
`@TransactionalEventListener(AFTER_COMMIT)` actualiza la orden a `PAID`, crea la
notificación in-app y envía el mail. Se elimina el mail "fantasma" dentro de la
transacción.

**Command + idempotencia**

`ChargePaymentCommand` lleva una `idempotencyKey`. La tabla `idempotency_keys`
guarda el resultado por clave. Un reintento del front devuelve el mismo resultado en
lugar de cobrar dos veces.

**Abstract Factory (opcional)**

Solo si el proveedor requiere una familia:

```java
public interface PaymentProviderFactory {
    PaymentGateway gateway();
    WebhookParser webhookParser();
    SignatureValidator signatureValidator();
    RefundService refundService();
}
```

Con un solo proveedor, no se agrega.

**Webhooks**

- `PaymentWebhookController` recibe el callback del proveedor.
- `WebhookParser` (Adapter) traduce el payload del proveedor a un evento interno.
- `SignatureValidator` (Strategy) valida la firma.
- El procesamiento es idempotente (Command + Registry).
- Nunca se confía en el payload sin validar firma y monto.

**Saga / Outbox**

Con pagos reales, la consistencia entre pago, orden y stock no se resuelve con una
transacción distribuida. Se usa **transactional outbox** + reintentos + compensación
(reembolso). Va en Fase 5.

#### 5.8.7 Otros escenarios del sistema

| Escenario | Patrones | Diseño |
|---|---|---|
| Precios | Strategy + Chain + Specification | `RetailPriceRule`, `WholesalePriceRule`, `OfferPriceRule`; gana la primera que aplica |
| Promociones | Composite + Specification | Descuentos combinables (porcentaje + 2x1 + monto fijo) |
| Checkout | Facade + Mediator + Chain + Template Method + Command | `CheckoutFacade` coordina; cadena de validaciones; comando idempotente |
| Stock | Command + Template Method + Decorator | `AdjustStockCommand` con undo; decorador de auditoría |
| Catálogo | Facade + Proxy + Builder + Specification | `CatalogFacade`; proxy de cache; filtros con Specification |
| Notificaciones | Observer + Adapter + Bridge + Decorator + Null Object | Evento → canal con retry y logs |
| Envío por zona | Strategy + Adapter | `ShippingCostStrategy` + `NominatimAdapter` |
| Reportes / CSV | Visitor + Factory Method | Exportadores por formato sin tocar el modelo |
| Repetir pedido | Prototype | Clonar una orden anterior como borrador |
| Historial de estados | Memento | `OrderStatusHistory` con snapshots |

#### 5.8.8 Anti-patrones y límites

- **No** implementar Singleton a mano: Spring ya lo hace.
- **No** usar Abstract Factory con un solo proveedor.
- **No** usar Flyweight con entidades JPA: son mutables y gestionadas.
- **No** usar Visitor para todo; solo cuando la estructura es estable y las
  operaciones crecen.
- **No** combinar Bridge y Strategy sin necesidad.
- **No** crear una interfaz por cada clase "porque SOLID".
- **No** aplicar un patrón sin un test que demuestre el comportamiento.

---

## 6. Plan de ejecución por fases

### Fase 0 — Fork, baseline y red de seguridad

**Objetivo:** partir de un estado conocido y protegido.

**Tareas:**

- [ ] Crear fork y rama `feat/pase-vinoteca`.
- [ ] Verificar que el proyecto compila (`./mvnw clean package`).
- [ ] Verificar que levanta con Docker Compose.
- [ ] Escribir tests de caracterización de los invariantes:
  - [ ] stock no negativo en checkout;
  - [ ] no comprar más que el stock;
  - [ ] total congelado al crear la orden;
  - [ ] cancelación repone stock y genera `CANCELLATION`;
  - [ ] precio mayorista se aplica desde la cantidad mínima;
  - [ ] pago aprobado pasa la orden a `PAID`;
  - [ ] pago rechazado cancela y repone stock.
- [ ] Documentar el estado inicial en este archivo (sección 3).

**Criterio de aceptación:** `./mvnw test` verde con al menos 7 tests de invariantes.

**Riesgo:** bajo. No se toca producción.

---

### Fase 1 — Renombre al vocabulario de la vinoteca

**Objetivo:** que el código hable de vino, bodega, tipo, distribuidor, varietal y
formato. Esta fase es **mecánica**: no cambia comportamiento.

**Tareas:**

- [ ] Renombrar paquete base `com.utn.corralon` → `com.utn.vinoteca`.
- [ ] Renombrar artefacto `corralon` → `vinoteca` en `pom.xml`.
- [ ] Renombrar `CorralonApplication` → `VinotecaApplication`.
- [ ] Renombrar base de datos por defecto `corralon_db` → `vinoteca_db`.
- [ ] Actualizar `application.yaml`, `compose.yaml`, contenedores y red.
- [ ] Renombrar entidades:
  - [ ] `ProductEntity` → `WineEntity`
  - [ ] `ProductVariantEntity` → `WineVariantEntity`
  - [ ] `BrandEntity` → `WineryEntity`
  - [ ] `CategoryEntity` → `WineTypeEntity`
  - [ ] `SupplierEntity` → `DistributorEntity`
- [ ] Renombrar DTOs, mappers, repositorios, servicios y controllers según la
  tabla 1.1.
- [ ] Renombrar paquetes `features/product`, `features/productVariant`,
  `features/brand`, `features/category`, `features/supplier`.
- [ ] Renombrar permisos `PRODUCT_*`, `PRODUCT_VARIANT_*`, `BRAND_*`,
  `CATEGORY_*`, `SUPPLIER_*`.
- [ ] Renombrar endpoints `/api/products`, `/api/product-variants`, `/api/brands`,
  `/api/categories`, `/api/suppliers`.
- [ ] Renombrar tablas y columnas de relación (`product_id` → `wine_id`,
  `product_variant_id` → `wine_variant_id`).
- [ ] Renombrar campos de relación y colecciones en Java (`product` → `wine`,
  `productVariant` → `wineVariant`, `products` → `wines`,
  `productVariants` → `wineVariants`, `brand` → `winery`, `category` →
  `wineType`, `supplier` → `distributor`).
- [ ] Actualizar textos de mails y `OpenApiConfig`.
- [ ] Actualizar `README.md`.
- [ ] Buscar y eliminar referencias a "corralón", "producto", "marca",
  "categoría" y "proveedor" en el vocabulario del código.

**Criterio de aceptación:**

- `grep -riE "corralon|ProductEntity|BrandEntity|CategoryEntity|SupplierEntity"`
  en `src/` no devuelve resultados.
- La app levanta y los tests pasan.
- Los endpoints nuevos responden igual que los viejos.

**Riesgo:** bajo (cambio mecánico). Hacerlo en un commit aislado para poder
revertir sin arrastrar lógica.

> **Nota sobre migración de datos:** si ya existen datos en MySQL, el renombre de
> tablas y columnas requiere un script SQL o Flyway. Para el fork de tesis, se
> recomienda recrear la base y volver a seedear.

---

### Fase 2 — Dominio vino

**Objetivo:** incorporar varietales y atributos de vino sin tocar el resto.

**Tareas:**

- [ ] Crear `GrapeVarietyEntity` + repository.
- [ ] Crear `GrapeVarietyService` + `IGrapeVarietyService`.
- [ ] Crear `GrapeVarietyController`, DTOs y mapper.
- [ ] Agregar permisos `GRAPE_VARIETY_*` a `Permits` y al seed de roles.
- [ ] Agregar campos `vintage`, `region`, `alcohol`, `tastingNotes` a `WineEntity`.
- [ ] Agregar relación N:M `grapeVarieties`.
- [ ] Actualizar `WineRequestDTO` y `WineResponseDTO`.
- [ ] Actualizar `WineMapper`.
- [ ] Actualizar `WineService.create/update` para resolver varietales.
- [ ] Actualizar `WineSpecification` con filtros `grapeVarietyId`, `vintage`,
  `region`.
- [ ] Actualizar `WineController.search` con los parámetros nuevos.
- [ ] Crear `WineDataInitializer` idempotente con tipos, varietales, bodegas,
  distribuidores, vinos y formatos de ejemplo.
- [ ] Actualizar Swagger y documentación.

**Criterio de aceptación:**

- Se puede crear un vino con 1..N varietales.
- Se puede filtrar por varietal, añada y región.
- El seed no duplica datos al reiniciar.
- Los tests de la Fase 0 siguen verdes.
- El resto de los endpoints funciona sin cambios.

**Riesgo:** medio. El único riesgo real es el mapeo N:M y la serialización
(evitar recursión infinita en DTOs; usar DTOs de resumen).

---

### Fase 3 — Saneo SOLID/GRASP

**Objetivo:** delegar mejor las responsabilidades sin cambiar comportamiento.

**Orden recomendado (cada paso con test):**

- [ ] Mover `decrementStock`/`restoreStock` a `WineVariantEntity`.
- [ ] Extraer `PricingService` y reemplazar las dos copias.
- [ ] Extraer `StockService` y reemplazar las tres copias.
- [ ] Extraer `OrderStatusPolicy` y usarlo en pago/cancelación.
- [ ] Extraer `OrderFactory` (creación de orden + ítems).
- [ ] Extraer `CheckoutService` + `ICheckoutService`; romper Cart → Order concreto.
- [ ] Extraer `PaymentGateway` + `SimulatedCardGateway`.
- [ ] Unificar `@Transactional` a Spring en todo el proyecto.
- [ ] `ErrorResponse` unificado y `GlobalExceptionHandler` sin strings.
- [ ] Eliminar `System.out.println` y usar `@Slf4j`.
- [ ] Corregir los bugs 1–9 de la tabla 3.9.
- [ ] Aplicar **Strategy + Factory/Registry** en `PaymentGateway`.
- [ ] Aplicar **State** en `OrderStatusPolicy` y `PaymentStatePolicy`.
- [ ] Aplicar **Observer** para los eventos de dominio.
- [ ] Aplicar **Facade/Mediator** en `CheckoutService`.
- [ ] Aplicar **Command + idempotencia** en checkout y pago.

**Criterio de aceptación:**

- Cero duplicación de cálculo de precio y de movimiento de stock.
- `CartServiceImpl` no importa `OrderService`.
- Todos los errores devuelven JSON estructurado.
- Tests de invariantes y de los bugs corregidos en verde.
- Contrato HTTP sin cambios (salvo los errores, que pasan a JSON).

**Riesgo:** medio-alto. Es la fase con más refactor. Se hace en commits pequeños y
revisables; no se avanza si un test falla.

---

### Fase 4 — Funcionalidades del plan de mejoras

**Objetivo:** cerrar los requerimientos pendientes.

**Tareas (en orden sugerido):**

- [ ] Paginación con `PageResponse<T>` en todos los listados.
- [ ] Catálogo público (`permitAll` en GET de catálogo).
- [ ] Refresh token real + rotación + revocación.
- [ ] Forgot/reset password.
- [ ] Admin seedeado.
- [ ] Notificaciones event-driven después del commit.
- [ ] Máquina de estados + número legible + historial de órdenes.
- [ ] Idempotencia en checkout y en pago.
- [ ] Guard de pago duplicado.
- [ ] Geocoding con Nominatim + campos de dirección.
- [ ] Imágenes con Cloudinary (`WineImageEntity`).
- [ ] Flyway (reemplazar `ddl-auto: update`).
- [ ] Arreglar N+1.
- [ ] **Adapter** para MercadoPago, Nominatim y Cloudinary.
- [ ] **Decorator** para retry, cache, logs e idempotencia.
- [ ] **Proxy** para cache de catálogo y rate limit.
- [ ] **Composite + Specification** para promociones.
- [ ] **Chain of Responsibility** para las validaciones de checkout.

**Criterio de aceptación:** cada feature con sus tests y su documentación Swagger.

**Riesgo:** medio. Se puede paralelizar por feature si el equipo lo permite.

---

### Fase 5 — Extras y cierre de tesis

- [ ] MercadoPago real.
- [ ] Costo de envío por zona.
- [ ] Observabilidad (Actuator + logs estructurados).
- [ ] Cache de catálogo.
- [ ] Rate limiting y `/api/v1`.
- [ ] Auditoría.
- [ ] Import CSV.
- [ ] **Prototype** para "repetir pedido".
- [ ] **Visitor** para reportes y exportaciones.
- [ ] **Saga/Outbox** para consistencia de pagos reales.
- [ ] Documentación final y defensa.

---

## 7. Estrategia de tests

### 7.1 Qué testear primero (Fase 0)

| Test | Tipo | Qué prueba |
|---|---|---|
| `StockNeverNegativeTest` | Integración | Checkout concurrente no deja stock negativo |
| `CannotBuyMoreThanStockTest` | Integración | Checkout con cantidad > stock falla |
| `OrderTotalFrozenTest` | Integración | Cambiar precio no altera orden creada |
| `CancelRestoresStockTest` | Integración | Cancelar repone y genera `CANCELLATION` |
| `WholesalePriceTest` | Unitario | Mayorista desde la cantidad mínima, sobre el total |
| `PaymentApprovedTest` | Integración | Pago aprobado → `PAID` |
| `PaymentRejectedTest` | Integración | Pago rechazado → `CANCELLED` + stock |

### 7.2 Herramientas

- JUnit 5 (incluido en `spring-boot-starter-*-test`).
- Mockito para unitarios.
- `@SpringBootTest` + Testcontainers o H2 en modo MySQL para integración.
- `MockMvc` para controllers.
- En lo posible, un `application-test.yaml` con base en memoria y Mailhog
  deshabilitado.

### 7.3 Regla de oro

> Ninguna extracción de la Fase 3 se mergea sin un test que demuestre que el
> comportamiento anterior se conserva.

---

## 8. Criterios de aceptación

### 8.1 Del pase a vinoteca

- [ ] Un vino puede tener 0..N varietales.
- [ ] Se puede filtrar el catálogo por bodega, tipo, varietal, añada y región.
- [ ] Los formatos se manejan como `WineVariantEntity` con stock y precio propios.
- [ ] El precio mayorista funciona por cantidad.
- [ ] El carrito, checkout, pago, cancelación y stock funcionan sin cambios.
- [ ] El seed carga datos de ejemplo sin duplicar.
- [ ] No queda ninguna referencia a "corralón" en código, config o docs.
- [ ] No quedan nombres genéricos (`ProductEntity`, `BrandEntity`,
  `CategoryEntity`, `SupplierEntity`, `ProductVariantEntity`) en el código.

### 8.2 De la arquitectura

- [ ] No hay lógica de precio duplicada.
- [ ] No hay lógica de stock duplicada.
- [ ] Las entidades protegen sus invariantes (`decrementStock`).
- [ ] `Cart` no depende de `OrderService` concreto.
- [ ] El pago se resuelve por Strategy.
- [ ] Todos los errores devuelven JSON estructurado.
- [ ] Se usa un solo `@Transactional` (Spring).
- [ ] `./mvnw test` verde.

### 8.3 Generales

- [ ] La app levanta con Docker Compose.
- [ ] Swagger documenta todos los endpoints.
- [ ] `README.md` actualizado.
- [ ] `PLAN_DE_PASE_VINOTECA.md` actualizado con lo ejecutado.

---

## 9. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Refactor de Fase 3 rompe comportamiento | Alto | Tests de invariantes antes; commits pequeños |
| Mapeo N:M de varietales genera recursión en JSON | Medio | DTOs de resumen; no serializar entidades |
| Concurrencia de stock sigue sin locking | Alto | Fase 3/4: `@Version` o update atómico |
| Cambio de package/entidades rompe imports/config | Medio | Fase 1 aislada y mecánica; buscar referencias con grep |
| Renombre de tablas rompe datos existentes | Medio | Recrear base y seedear; o script SQL/Flyway |
| Seed duplica datos al reiniciar | Bajo | Inicializador idempotente |
| Se subestima el front (Angular) | Medio | Congelar contrato HTTP por fase |
| Fuga de secretos en `.env` | Alto | `.gitignore` + revisar antes de cada commit |
| Cambios de esquema sin migraciones | Medio | Flyway en Fase 4 |

---

## 10. Decisiones de diseño

### D1 — Vocabulario del core: resuelto

**Decisión:** se adoptan nombres de negocio en todo el núcleo del catálogo.
No se usan `ProductEntity`, `BrandEntity`, `CategoryEntity`, `SupplierEntity` ni
`ProductVariantEntity` en el sistema destino.

| Concepto | Nombre adoptado |
|---|---|
| Vino | `WineEntity` |
| Formato | `WineVariantEntity` |
| Bodega | `WineryEntity` |
| Tipo de vino | `WineTypeEntity` |
| Distribuidor | `DistributorEntity` |
| Varietal | `GrapeVarietyEntity` |

**Consecuencia:** la Fase 1 toca muchos archivos, pero es un renombre mecánico y
controlado. Se prioriza el lenguaje ubicuo y la claridad de la tesis por sobre el
mínimo churn absoluto.

### D2 — Añada a nivel vino o formato

| Opción | Cuándo conviene |
|---|---|
| **Vino** (recomendada) | Cada añada es una etiqueta distinta (lo habitual) |
| **Formato** | La misma etiqueta se vende en varias añadas simultáneamente |

**Recomendación:** `WineEntity.vintage`. Si aparece el segundo caso, mover a
`WineVariantEntity`.

### D3 — Región como texto o entidad

**Decisión:** texto (`WineEntity.region`) en Fase 2. Si el front necesita filtro por
entidad, promover a `RegionEntity` en una fase posterior.

### D4 — `GrapeVariety` N:M vs 1:N

**Decisión:** N:M. Un corte necesita varios varietales y un varietal participa en
muchos vinos.

---

## 11. Backlog consolidado

### 11.1 Estado por funcionalidad

| Funcionalidad | Estado actual | Pase a vinoteca | Plan de mejoras |
|---|---|---|---|
| Registro / login JWT | Implementado | Sin cambios | Refresh, reset, admin seed |
| Roles y permisos | Implementado | + permisos varietal | Unificar modelo |
| Usuarios | Implementado | Sin cambios | Paginación |
| Direcciones | Implementado | Sin cambios | Geocoding, provincia, lat/lng |
| Tipos de vino | Implementado (hoy `CategoryEntity`) | Renombre a `WineTypeEntity` | Paginación |
| Bodegas | Implementado (hoy `BrandEntity`) | Renombre a `WineryEntity` | Paginación |
| Distribuidores | Implementado (hoy `SupplierEntity`) | Renombre a `DistributorEntity` | Paginación |
| Vinos | Implementado (hoy `ProductEntity`) | Renombre a `WineEntity` + campos de vino + varietales | Imágenes, paginación, catálogo público |
| Formatos | Implementado (hoy `ProductVariantEntity`) | Renombre a `WineVariantEntity` | Locking de stock, paginación |
| Stock y movimientos | Implementado | Sin cambios | `StockService`, locking |
| Precio mayorista | Implementado | Sin cambios | `PricingService` |
| Carrito | Implementado | Sin cambios | Paginación, userExternalId |
| Órdenes | Implementado | Sin cambios | Estados, historial, idempotencia |
| Pagos | Implementado (simulado) | Sin cambios | Strategy, MercadoPago |
| Ofertas | Implementado | Sin cambios | Fix path, activación por fecha |
| Notificaciones in-app | Implementado (huérfano) | Sin cambios | Event-driven |
| Emails | Implementado | Cambio de identidad | Después del commit |
| **Varietales** | **No existe** | **Nuevo** | — |
| **Añada / región / notas** | **No existe** | **Nuevo** | — |

### 11.2 Orden recomendado

1. Fase 0 (tests).
2. Fase 1 (renombre completo al vocabulario de vino).
3. Fase 2 (varietales y campos de vino).
4. Fase 3 (SOLID/GRASP).
5. Fase 4 (mejoras).
6. Fase 5 (extras).

---

## 12. Apéndices

### A. Endpoints actuales (nombres destino)

| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/auth/login` | Público |
| POST | `/api/auth/register` | Público |
| POST | `/api/users` | `USER_CREATE` |
| GET | `/api/users` | `USER_LIST` |
| GET | `/api/users/{externalId}` | `USER_READ` |
| PUT | `/api/users/{externalId}` | `USER_UPDATE` |
| DELETE | `/api/users/{externalId}` | `USER_DELETE` |
| POST | `/api/addresses` | `ADDRESS_CREATE` |
| GET | `/api/addresses` | `ADDRESS_LIST` |
| GET | `/api/addresses/{externalId}` | `ADDRESS_READ` |
| PUT | `/api/addresses/{externalId}` | `ADDRESS_UPDATE` |
| DELETE | `/api/addresses/{externalId}` | `ADDRESS_DELETE` |
| GET | `/api/addresses/user/{userExternalId}` | `ADDRESS_LIST_BY_USER` |
| POST/GET/PUT/DELETE | `/api/wine-types/**` | `WINE_TYPE_*` |
| POST/GET/PUT/DELETE | `/api/wineries/**` | `WINERY_*` |
| POST/GET/PUT/DELETE | `/api/distributors/**` | `DISTRIBUTOR_*` |
| POST/GET/PUT/DELETE | `/api/grape-varieties/**` | `GRAPE_VARIETY_*` |
| GET | `/api/wines/{externalId}` | `WINE_READ` |
| POST | `/api/wines` | `WINE_CREATE` |
| PUT | `/api/wines/{externalId}` | `WINE_UPDATE` |
| DELETE | `/api/wines/{externalId}` | `WINE_DELETE` |
| PATCH | `/api/wines/{externalId}/activate` | `WINE_ACTIVATE` |
| GET | `/api/wines/search` | `WINE_LIST` |
| GET | `/api/wines/inactive` | `WINE_LIST_INACTIVE` |
| GET | `/api/wine-variants/{externalId}` | `WINE_VARIANT_READ` |
| POST | `/api/wine-variants` | `WINE_VARIANT_CREATE` |
| PUT | `/api/wine-variants/{externalId}` | `WINE_VARIANT_UPDATE` |
| DELETE | `/api/wine-variants/{externalId}` | `WINE_VARIANT_DELETE` |
| PATCH | `/api/wine-variants/{externalId}/activate` | `WINE_VARIANT_ACTIVATE` |
| GET | `/api/wine-variants/search` | `WINE_VARIANT_LIST` |
| GET | `/api/wine-variants/inactive` | `WINE_VARIANT_LIST_INACTIVE` |
| POST | `/api/wine-variants/stock/entry` | `STOCK_ENTRY` |
| POST | `/api/wine-variants/stock/adjustment` | `STOCK_ADJUSTMENT` |
| GET | `/api/wine-variants/{externalId}/stock` | `STOCK_READ` |
| GET | `/api/stock-movements/variant/{variantId}` | `STOCK_MOVEMENT_LIST_BY_VARIANT` |
| GET | `/api/stock-movements/{externalId}` | `STOCK_MOVEMENT_READ` |
| POST | `/api/carts` | `CART_CREATE_OR_UPDATE` |
| GET | `/api/carts/{userId}` | `CART_READ` |
| PATCH | `/api/carts/{userId}/items/{variantId}` | `CART_UPDATE_ITEM_QUANTITY` |
| DELETE | `/api/carts/{userId}` | `CART_CLEAR` |
| POST | `/api/carts/{userId}/checkout` | `CART_CHECKOUT` |
| GET | `/api/orders/admin` | `ADMIN` o `EMPLOYEE` |
| GET | `/api/orders/{externalId}` | `ORDER_READ` |
| POST | `/api/orders/{externalId}/cancel` | `ORDER_CANCEL_OWN` |
| GET | `/api/orders/user/{userExternalId}` | `ORDER_READ_BY_USER` |
| GET | `/api/orders/admin/{externalId}` | `ADMIN` o `EMPLOYEE` |
| POST | `/api/payments/pay` | `PAYMENT_CREATE` |
| GET | `/api/payments/order/{orderId}` | `PAYMENT_READ` |
| POST/GET/PUT/DELETE | `/api/offers/**` | `OFFER_*` |
| POST/GET/PATCH/DELETE | `/api/notifications/**` | `NOTIFICATION_*` |

### B. Variables de entorno

| Variable | Uso | Default |
|---|---|---|
| `DB_HOST` | Host MySQL | `localhost` |
| `DB_PORT` | Puerto MySQL | `3306` |
| `DB_NAME` | Nombre de base | `vinoteca_db` |
| `DB_USER` | Usuario | `user` |
| `DB_PASSWORD` | Contraseña | `password` |
| `MYSQL_ROOT_PASSWORD` | Root de MySQL | `root` |
| `JWT_SECRET` | Firma JWT | Requerido |
| `JWT_EXPIRATION` | Expiración access token (ms) | Según `.env` |
| `JWT_REFRESH_EXPIRATION` | Expiración refresh token (ms) | `86400000` |
| `MAIL_HOST` | SMTP host | `localhost` |
| `MAIL_PORT` | SMTP puerto | `1025` |
| `MAIL_USERNAME` | SMTP usuario | Vacío |
| `MAIL_PASSWORD` | SMTP password | Vacío |
| `MAIL_AUTH` | SMTP auth | `false` |
| `MAIL_STARTTLS` | SMTP starttls | `false` |
| `APP_BASE_URL` | Base para links de mails | `http://localhost:8080` |

### C. Checklist de fork

- [ ] Fork creado y clonado.
- [ ] `upstream` configurado.
- [ ] Rama `feat/pase-vinoteca` creada.
- [ ] `.env` propio creado (no commitear).
- [ ] `./mvnw clean package` OK.
- [ ] `docker compose up` OK.
- [ ] Swagger responde en `/swagger-ui.html`.
- [ ] Tests de Fase 0 escritos y verdes.
- [ ] Este documento copiado al fork y actualizado con el nombre del repo.

### D. Glosario

| Término | Significado |
|---|---|
| **Añada / Vintage** | Año de cosecha de la uva |
| **Varietal** | Uva o conjunto de uvas de un vino |
| **Corte / Blend** | Vino compuesto por más de un varietal |
| **Bodega** | Productor del vino (`WineryEntity`) |
| **Formato** | Presentación comercial (750 ml, magnum, caja) (`WineVariantEntity`) |
| **Mayorista** | Precio por volumen para reventa/gastronomía |
| **Soft delete** | Baja lógica mediante `active = false` |
| **externalId** | UUID público que reemplaza al id interno en la API |
| **Feature** | Módulo vertical del monolito (`features/wine`, etc.) |

### E. Referencias internas

- `REGLAS_DE_NEGOCIO.md` — reglas funcionales del sistema.
- `PLAN_DE_MEJORAS.md` — deuda técnica y roadmap de mejoras.
- `README.md` — puesta en marcha y variables de entorno.
- `compose.yaml` — infraestructura local.
- `pom.xml` — dependencias y versiones.

### F. Patrones por fase

| Patrón | SOLID/GRASP que materializa | Fase |
|---|---|---|
| Strategy (pricing, pago, envío) | OCP, Polymorphism, Protected Variations | 3 |
| State (orden, pago) | SRP, Information Expert | 3 |
| Observer (eventos) | Low Coupling, Indirection | 3 |
| Facade / Mediator (checkout) | SRP, Low Coupling | 3 |
| Template Method (pago, checkout) | Protected Variations | 3 |
| Command + idempotencia | Pure Fabrication, SRP | 3–4 |
| Adapter (MercadoPago, Nominatim, Cloudinary) | DIP, Protected Variations | 4 |
| Decorator (retry, cache, logs) | OCP, Protected Variations | 4 |
| Proxy (cache, rate limit) | Protected Variations | 4 |
| Composite (promociones) | Information Expert | 4 |
| Chain of Responsibility (validaciones) | SRP, Low Coupling | 4 |
| Abstract Factory (familia de pago) | OCP, Creator | 4 (opcional) |
| Prototype (repetir pedido) | Creator | 5 |
| Visitor (reportes) | OCP | 5 |
| Saga / Outbox | Protected Variations | 5 |

---

> Fin del documento. Cualquier cambio de alcance debe reflejarse acá antes de
> implementarse.
