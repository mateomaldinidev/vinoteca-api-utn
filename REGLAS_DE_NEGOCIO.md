# Corralón UTN — Documentación de Proyecto



> Especificación funcional, reglas de negocio y decisiones técnicas  
> Última actualización: Junio 2026

---

## Índice

1. [Descripción General](#1-descripción-general)
2. [Roles del Sistema](#2-roles-del-sistema)
3. [Gestión de Usuarios](#3-gestión-de-usuarios)
4. [Gestión de Direcciones](#4-gestión-de-direcciones)
5. [Gestión de Productos](#5-gestión-de-productos)
6. [Gestión de Variantes](#6-gestión-de-variantes)
7. [Gestión de Stock](#7-gestión-de-stock)
8. [Movimientos de Stock](#8-movimientos-de-stock)
9. [Precio Mayorista](#9-precio-mayorista)
10. [Carrito](#10-carrito)
11. [Órdenes](#11-órdenes)
12. [Cancelación de Órdenes](#12-cancelación-de-órdenes)
13. [Promociones](#13-promociones)
14. [Notificaciones por Mail](#14-notificaciones-por-mail)
15. [Invariantes del Sistema](#15-invariantes-del-sistema)
16. [Decisiones Técnicas](#16-decisiones-técnicas)
17. [Objetivo General](#17-objetivo-general)

---

## 1. Descripción General

El sistema es una plataforma de **ecommerce** destinada a la venta de materiales de construcción y productos de corralón.

**Funcionalidades principales:**
- Registro y autenticación de usuarios
- Navegación del catálogo de productos
- Carrito de compras persistente
- Generación y seguimiento de órdenes de compra
- Administración de productos, variantes, stock y órdenes mediante perfiles con privilegios

---

## 2. Roles del Sistema

El sistema define cuatro roles. Cada usuario autenticado puede tener **uno o más roles** simultáneamente. Esto permite, por ejemplo, que un vendedor (EMPLOYEE) también pueda comprar como cliente (CUSTOMER).

> Los roles se gestionan a través de `CredentialsEntity`, que mantiene una relación `@ManyToMany` con `RoleEntity`. Spring Security lee los roles directamente de esta relación para la autorización.

### 🔴 ADMIN
Acceso total al sistema.

| Puede | No puede |
|---|---|
| Crear usuarios ADMIN y EMPLOYEE | — |
| Gestionar productos, variantes y stock | — |
| Gestionar promociones | — |
| Gestionar y modificar estados de órdenes | — |
| Acceder a toda la información del sistema | — |

### 🟠 EMPLOYEE
Permisos operativos sobre el catálogo y las órdenes.

| Puede | No puede |
|---|---|
| Crear, modificar y dar de baja productos | Crear usuarios ADMIN |
| Crear y modificar variantes | Crear usuarios EMPLOYEE |
| Gestionar stock | — |
| Gestionar promociones | — |
| Visualizar y modificar estados de órdenes | — |

### 🟢 CUSTOMER
Usuario autenticado y registrado.

| Puede | No puede |
|---|---|
| Mantener sus direcciones | Gestionar productos o stock |
| Gestionar su carrito | Ver órdenes de otros usuarios |
| Crear órdenes | Modificar estados de órdenes |
| Consultar y cancelar sus propias órdenes (según reglas) | — |

### ⚪ GUEST
Usuario no autenticado.

| Puede | No puede |
|---|---|
| Navegar el catálogo | Crear órdenes |
| Consultar productos | Gestionar carrito persistente |
| — | Acceder a información privada |

---

## 3. Gestión de Usuarios

### Registro
- Cualquier persona puede registrarse libremente.
- Todo usuario registrado obtiene automáticamente al menos un rol (por defecto **ROLE_CUSTOMER**).
- Al registrarse se crea un `UserEntity` (datos personales) y un `CredentialsEntity` (credenciales de autenticación) vinculados entre sí.

### Autenticación
- El sistema utiliza **Spring Security** con tokens **JWT** (JSON Web Tokens).
- El login se realiza mediante **email** (username) y **password**.
- Las contraseñas se encriptan con **BCrypt** — nunca se almacenan en texto plano.
- Tras un login exitoso se devuelven dos tokens:
  - **Access Token**: JWT de corta duración (1 hora por defecto). Se envía en el header `Authorization: Bearer <token>` en cada request protegido.
  - **Refresh Token**: JWT de larga duración (24 horas por defecto). Se almacena en la base de datos y se usa para obtener nuevos access tokens sin reenviar credenciales.
- El refresh token se renueva automáticamente cada vez que se usa el endpoint `/api/auth/refresh`.
- Un usuario dado de baja (`enabled = false` en `CredentialsEntity`) **no puede autenticarse**.

### Administración
- La eliminación de usuarios se realiza mediante **baja lógica** (no se eliminan registros de la base de datos).
- Al desactivar un usuario, se desactivan tanto `UserEntity.active = false` como `CredentialsEntity.enabled = false`.

### Reglas
- El email debe ser **único** — no pueden existir dos usuarios con el mismo email.
- Todo usuario autenticado debe tener **al menos un rol** asignado en `CredentialsEntity`.
- Los roles se gestionan en `CredentialsEntity`, no en `UserEntity`. `UserEntity` no almacena información de roles.

---

## 4. Gestión de Direcciones

- Un usuario puede registrar **múltiples direcciones**.
- Cada dirección queda asociada a un único usuario.

### Reglas

| Regla | Detalle |
|---|---|
| Una dirección pertenece a un único usuario | Relación many-to-one |
| Un usuario puede tener cero o más direcciones | No es obligatorio tener dirección |
| Órdenes de **envío** requieren dirección válida | No se puede generar orden de envío sin dirección |
| Órdenes de **retiro en sucursal** no requieren dirección | El campo puede estar vacío |

---

## 5. Gestión de Productos

Los productos representan los artículos comercializables del corralón.

**Ejemplos:** Cemento, Arena, Pintura, Cerámicos, etc.

### Baja lógica
La eliminación se realiza mediante el campo `active`.

Un producto **inactivo**:
- No aparece en el catálogo público.
- No puede comprarse.
- Conserva su historial asociado (trazabilidad).

### Reglas

| Regla |
|---|
| Todo producto debe tener **nombre** |
| La descripción es **opcional** |
| Un producto puede tener múltiples variantes |
| Un producto **sin variantes no puede venderse** |

---

## 6. Gestión de Variantes

Las variantes representan las **presentaciones comerciales** de un producto.

**Ejemplo:**

```
Producto:  Cemento Portland
Variantes: Bolsa 25kg
           Bolsa 50kg
```

Otros ejemplos de diferenciación: color, tamaño, capacidad, presentación.

### Reglas

| Regla |
|---|
| Toda variante pertenece a **un único producto** |
| Toda variante posee **stock propio** |
| Toda variante posee **precio propio** |
| Toda variante puede tener configuración de **precio mayorista** |
| El stock se controla **exclusivamente a nivel de variante** |

---

## 7. Gestión de Stock

El stock se controla sobre cada **variante** de forma individual.

### Restricciones críticas

> ⚠️ **Stock negativo: PROHIBIDO**  
> Nunca puede existir stock negativo. Toda operación debe validar disponibilidad antes de descontar unidades.

> ⚠️ **Compra sin stock: PROHIBIDA**  
> No se pueden generar órdenes que superen el stock disponible.

---

## 8. Movimientos de Stock

Todo cambio de stock debe registrarse obligatoriamente mediante un **movimiento**.

### Tipos de movimiento permitidos

| Tipo | Descripción |
|---|---|
| `ENTRY` | Ingreso de mercadería |
| `SALE` | Descuento por venta (orden generada) |
| `CANCELLATION` | Reposición por cancelación de orden |
| `ADJUSTMENT` | Ajuste manual de stock |

### Reglas
- Todo cambio de stock **debe generar un movimiento** — sin excepción.
- **No está permitido** modificar el stock directamente sin registrar el movimiento correspondiente.

---

## 9. Precio Mayorista

Las variantes pueden configurar precio mayorista con dos campos:

| Campo | Descripción |
|---|---|
| `wholesalePrice` | Precio a aplicar cuando se cumple la condición mayorista |
| `wholesaleMinQty` | Cantidad mínima a partir de la cual aplica el precio mayorista |

### Regla principal
Cuando la cantidad comprada **alcanza o supera** `wholesaleMinQty`, el precio mayorista se aplica a la **totalidad de las unidades** compradas.

### Ejemplo

```
Precio minorista:    $100
Precio mayorista:    $80
Cantidad mínima:     10 unidades

Compra de 10 unidades → Total: 10 × $80 = $800  ✅ (aplica mayorista)
Compra de  9 unidades → Total:  9 × $100 = $900  (precio normal)
```

---

## 10. Carrito

Cada usuario posee **un único carrito activo** que es **persistente** (se mantiene entre sesiones).

### Reglas

| Regla |
|---|
| Un usuario tiene **un único carrito** |
| Un carrito pertenece a **un único usuario** |
| El carrito puede contener **múltiples ítems** |
| El carrito **no reserva ni modifica stock** |
| El stock se descuenta **únicamente al generar una orden** |

---

## 11. Órdenes

Las órdenes representan **compras confirmadas**.

### Datos almacenados por orden

- Usuario propietario
- Dirección de envío (o indicación de retiro en sucursal)
- Total calculado (queda **congelado** al momento de creación)
- Estado actual
- Fecha de creación

### Estados y flujo

```
PENDING_PAYMENT → PAID
       ↓
   CANCELLED  (puede cancelarse desde PENDING_PAYMENT)
```

| Estado | Descripción |
|---|---|
| `PENDING_PAYMENT` | Estado inicial de toda orden — pendiente de pago |
| `PAID` | Orden pagada — no puede modificarse ni cancelarse |
| `CANCELLED` | Orden cancelada — no puede volver a estados anteriores |

### Reglas

- Toda orden inicia en **`PENDING_PAYMENT`**.
- Una orden **`CANCELLED`** no puede volver a estados anteriores.
- Una orden **`PAID`** se considera finalizada (no modificable ni cancelable).
- Una orden en **`PENDING_PAYMENT`** puede ser cancelada.
- El **total queda congelado** al momento de crear la orden (no varía si el precio cambia luego).

---

## 12. Cancelación de Órdenes

### ¿Quién puede cancelar?

| Rol | Puede cancelar |
|---|---|
| **CUSTOMER** | Solo sus propias órdenes, cuando las reglas lo permitan |
| **EMPLOYEE** | Cualquier orden |
| **ADMIN** | Cualquier orden |

### Recuperación de stock
Toda cancelación debe:
1. **Devolver automáticamente** el stock correspondiente a cada variante.
2. Registrar un movimiento de stock de tipo **`CANCELLATION`**.

---

## 13. Promociones

Las promociones son administradas por **EMPLOYEE** y **ADMIN**.

### Reglas

| Regla |
|---|
| Deben poder **activarse y desactivarse** |
| Deben conservar **historial** (baja lógica) |
| **Nunca** deben modificar el precio base de una variante |

---

## 14. Notificaciones por Mail

El sistema envía notificaciones automáticas por correo electrónico ante determinados eventos.

### Eventos que disparan notificaciones

| Evento | Destinatario | Detalle |
|---|---|---|
| Registro de usuario | CUSTOMER | Bienvenida y confirmación de cuenta |
| Contraseña modificada | CUSTOMER | Cambio de contraseña exitoso |
| Orden creada | CUSTOMER | Confirmación de la orden con detalle de ítems y total |
| Cambio de estado de orden | CUSTOMER | Notificación cuando la orden pasa a `PAID` o `CANCELLED` |
| Cancelación de orden | CUSTOMER | Notificación con motivo de cancelación (si aplica) |
| Nueva promoción activa | CUSTOMER | Notificacion promocional cuando se lanza una nueva oferta |

### Decisiones técnicas para mail

- El envío de mails debe ser **asincrónico** — no debe bloquear la respuesta al cliente.
- Se utiliza **Spring Mail** (`spring-boot-starter-mail`) con un servidor SMTP configurable por variables de entorno.
- Los emails se envían en **formato texto plano**. En el futuro pueden migrarse a templates HTML (Thymeleaf o similar).
- En entornos de desarrollo/testing se usa **Mailhog** para interceptar correos sin enviarlos realmente.
- Los errores en el envío de mail **no deben propagar excepciones** que rompan el flujo principal de negocio — deben loguearse y manejarse silenciosamente.

### Configuración sugerida (variables de entorno)

```properties
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=noreply@corralonutn.com
MAIL_PASSWORD=*****
MAIL_FROM=noreply@corralonutn.com
```

---

## 15. Invariantes del Sistema

Las siguientes reglas representan condiciones que **nunca deben romperse** bajo ninguna circunstancia:

| # | Invariante |
|---|---|
| 1 | No existe **stock negativo** |
| 2 | No existen **emails duplicados** |
| 3 | No existen órdenes **sin usuario** |
| 4 | No existen variantes **sin producto** |
| 5 | No existen carritos **sin usuario** |
| 6 | Toda modificación de stock **genera un movimiento** |
| 7 | No puede comprarse **más stock del disponible** |
| 8 | Toda orden pertenece a **un usuario** |
| 9 | Todo usuario posee **al menos un rol** en sus credenciales |
| 10 | Un producto **inactivo no puede venderse** |

---

## 16. Decisiones Técnicas

### DTOs
- **Nunca** exponer entidades JPA directamente en los endpoints.
- Todos los endpoints deben utilizar **DTOs** (Data Transfer Objects) para requests y responses.

### ModelMapper
- Todo mapeo entre `Entity` ↔ `DTO` debe realizarse usando **ModelMapper**.
- No deben realizarse conversiones manuales salvo casos excepcionales debidamente justificados.

### Capa de Servicios
Toda funcionalidad de negocio debe implementarse mediante el patrón:

```
Interface:      ProductService
Implementación: ProductServiceImpl
```

Esto aplica a todos los dominios del sistema.

### Repositories
- Los repositorios son responsables **exclusivamente del acceso a datos**.
- **No deben contener lógica de negocio.**

### Controllers
Los controllers son responsables únicamente de:
- Recibir requests HTTP
- Validar la entrada
- Invocar el servicio correspondiente
- Devolver la respuesta

> ⚠️ **No deben contener lógica de negocio.**

### Arquitectura de dependencias

```
Controller → Service → Repository
```

Las dependencias deben respetar **estrictamente esta dirección**. No se permiten dependencias inversas ni salteadas.

### Soft Delete (Baja Lógica)
- La eliminación funcional se implementa mediante baja lógica en todos los dominios donde exista información histórica asociada.
- **No deben eliminarse registros físicamente** cuando haya historial vinculado.

### Stack tecnológico
- Backend: **Spring Boot**
- Seguridad: **Spring Security** + **JWT** (jjwt 0.13.0)
- Encriptación de contraseñas: **BCrypt**
- ORM: **JPA / Hibernate**
- Mapeo: **ModelMapper**
- Mail: **Spring Mail** (asincrónico)
- Base de datos: configurable por entorno

### Arquitectura de Autenticación
- `UserEntity`: almacena datos personales del usuario (nombre, email, etc.). **No almacena roles**.
- `CredentialsEntity`: implementa `UserDetails` de Spring Security. Almacena username (email), password encriptado, estado (enabled), refreshToken y roles.
- `RoleEntity`: entidad que representa un rol del sistema con sus permisos asociados.
- `PermitEntity`: entidad que representa un permiso individual.
- Relación: `UserEntity` (1:1) `CredentialsEntity` (N:M) `RoleEntity` (N:M) `PermitEntity`.
- El `JWT_SECRET` se almacena exclusivamente en variables de entorno (`.env`) y **nunca** se sube al repositorio.
- Los endpoints protegidos requieren el header `Authorization: Bearer <access_token>`.
- El endpoint `/api/auth/refresh` permite renovar tokens usando el refresh token sin reenviar credenciales.

---

## 17. Objetivo General

> El sistema debe permitir administrar integralmente un ecommerce de corralón manteniendo **consistencia de stock**, **trazabilidad de movimientos**, **control de órdenes** y **gestión de productos** mediante una arquitectura escalable basada en **Spring Boot**.
