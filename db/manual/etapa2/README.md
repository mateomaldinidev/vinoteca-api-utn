# Etapa 2 - Scripts SQL manuales (propuesta)

Estado: **propuesta corregida y auditada estáticamente, NO ejecutada y NO aprobada para ejecución**. Ningún script queda aprobado por el hecho de estar corregido o documentado. La aprobación para ejecutar es explícita, script por script, y es previa a cualquier ejecución.

No se modificó ningún archivo Java, YAML ni de configuración. No se cambió `ddl-auto`. No se ejecutó ningún `ALTER`, `UPDATE`, `INSERT`, `DELETE`, `DROP` ni `TRUNCATE` sobre `corralon_db`.

Base inspeccionada: `corralon_db`, MySQL `8.0.46`, host `localhost:3307` (contenedor `corralon-mysql:3306`).

---

## 1. Archivos, orden y dependencias

| Orden | Archivo | Tipo | Efecto |
|---|---|---|---|
| 0 | `V2_00__prevalidacion.sql` | Solo lectura | Versión, colisiones, truncamiento de `GROUP_CONCAT`, prevalidación de tipos/unicidad de la FK compuesta, FK de usuario y datos incompatibles. Registra línea base. |
| 1 | `V2_01__orders_checkout.sql` | DDL | `expires_at`, `cancellation_reason`, `cancelled_at`, `idempotency_key`, `checkout_request_hash`, unique de idempotencia, índice de vencimiento. |
| 2 | `V2_02__orders_delivery_snapshot.sql` | DDL | Snapshot de dirección de entrega en `orders`. Conserva `address_id`. |
| 3 | `V2_03__order_items_snapshot.sql` | DDL | `product_name_snapshot`, `variant_attribute_snapshot`. |
| 4 | `V2_04__stock_movements_traceability.sql` | DDL | `order_id`, `order_item_id`, `resulting_stock`, `created_by_user_id`; `UNIQUE (id, order_id)` en `order_items`; FK compuesta; FKs e índices. |
| 5 | `V2_05__integrity_constraints.sql` | DDL | `CHECK stock >= 0`, `CHECK quantity > 0` en items, `CHECK quantity <> 0` en movimientos. |
| 6A | `V2_06A__opt_orders_idempotency_pair.sql` | DDL opcional | `CHECK` key/hash juntos o ambos NULL. Independiente. |
| 6B | `V2_06B__opt_orders_cancelled_coherence.sql` | DDL opcional | `CHECK` `cancelled_at`/motivo solo si `CANCELLED`. Independiente. |
| 6C | `V2_06C__opt_orders_cancellation_reason.sql` | DDL opcional | `CHECK` de motivos acordados. Independiente. |
| 6D | `V2_06D__opt_stock_movements_resulting_stock.sql` | DDL opcional | `CHECK resulting_stock >= 0` o NULL. Independiente. |
| 7 | `V2_99__postvalidacion.sql` | Solo lectura | Objetos obligatorios (resumen con `FALTANTE`), presencia de opcionales, huellas, NULLs y coherencia compuesta. |
| — | `V2_90__rollback.sql` | DDL de reversa | Revierte el esquema en orden inverso. **No recupera datos.** |
| — | `V2_98__opcional_test_en_clon.sql` | Prueba | Demuestra CHECK y FK compuesta. **Solo en clon descartable.** |

### Orden exacto

```
V2_00 (solo lectura)
  -> backup lógico completo y verificado
  -> V2_01
  -> V2_02
  -> V2_03
  -> V2_04
  -> V2_05
  -> (opcionales, solo los aprobados) V2_06A / V2_06B / V2_06C / V2_06D
  -> V2_99
```

`V2_90` solo se ejecuta si se decide revertir. `V2_98` no forma parte del orden de aplicación: es una guía de prueba para un clon.

### Dependencias

- `V2_01`, `V2_02` y `V2_03` dependen solo de `V2_00`.
- `V2_04` depende de `V2_01` (FK a `orders`) y de la tabla `order_items`. Se ejecuta después de `V2_03` por orden lógico, aunque no necesita sus columnas.
- `V2_05` depende de `V2_00` y se ejecuta después de `V2_04`.
- `V2_06A`, `V2_06B` y `V2_06C` dependen de `V2_01`.
- `V2_06D` depende de `V2_04`.
- `V2_06A..D` son independientes entre sí: se pueden aprobar y aplicar por separado.
- `V2_99` se ejecuta después de `V2_01..V2_05` y de los opcionales que se hayan aprobado.
- `V2_90` se ejecuta después de todo lo aplicado, en orden inverso.

---

## 2. Línea base observada (2026-11-10)

| Tabla | Filas |
|---|---:|
| `orders` | 4 |
| `order_items` | 8 |
| `stock_movements` | 17 |
| `product_variants` | 12 |
| `addresses` | 5 |
| `payments` | 2 |

- `orders`: 2 `CANCELLED`, 2 `PAID`, 0 `PENDING_PAYMENT`.
- `stock_movements`: 8 `SALE`, 4 `CANCELLATION`, 5 `ADJUSTMENT`.
- `product_variants.stock`: mínimo 3, máximo 4000. Sin negativos.
- `order_items.quantity`: sin cero ni negativos.
- `stock_movements.quantity`: sin cero.
- No existen CHECK constraints en el esquema actual.
- No existen las columnas, índices ni constraints que propone esta etapa.
- No hay duplicados que bloqueen las restricciones propuestas.
- `addresses` usadas por pedidos: 3. Ninguna inactiva entre las referenciadas.
- Longitudes actuales: `products.name` máx 33, `product_variants.attribute` máx 15, `stock_movements.reason` máx 500.
- La cancelación del pedido 2 ya fue aplicada en una operación anterior; no se repite en esta etapa.

Estos valores son una foto del entorno de desarrollo y pueden cambiar. `V2_00` los vuelve a medir; no deben asumirse como constantes.

---

## 3. Decisiones aprobadas

### 3.1. Idempotencia del checkout (D21 y D22)

La implementación Java de la idempotencia aún no existe; el checkout actual ya recibe `deliveryType` y `addressId`. El contrato aprobado es:

1. El header `Idempotency-Key` es **obligatorio** para pedidos nuevos. La API valida presencia y longitud; la base no impone un `NOT NULL` global para no romper pedidos históricos.
2. **Alcance por usuario**: `UNIQUE KEY uk_orders_user_idempotency (user_id, idempotency_key)`. La misma clave de dos usuarios distintos es independiente.
3. La clave se guarda tal cual la envía el cliente, con **máximo 255 caracteres**. La API debe rechazar claves más largas antes de llegar a la base.
4. El hash es **SHA-256 hexadecimal en minúsculas** (64 caracteres) y se guarda en `checkout_request_hash VARCHAR(128) ascii_bin`.
5. El hash se calcula sobre un **payload canónico** formado únicamente por:
   - `deliveryType` normalizado: `trim` y mayúsculas; debe ser `DELIVERY` o `PICKUP`.
   - `addressId`: el **UUID externo recibido en la request** (no el `orders.address_id` numérico), en formato UUID canónico minúsculas con guiones; para `PICKUP` es `null`; para `DELIVERY` es obligatorio.
6. Serialización canónica exacta: JSON UTF-8, sin espacios ni saltos de línea, con las claves en este orden fijo y sin claves adicionales:
   ```json
   {"deliveryType":"DELIVERY","addressId":"550e8400-e29b-41d4-a716-446655440000"}
   ```
   ```json
   {"deliveryType":"PICKUP","addressId":null}
   ```
7. **No se incluyen** los ítems del carrito, cantidades, precios, subtotales, total, `user_id`, timestamps ni headers.
8. Flujo obligatorio, en este orden:
   1. Validar `Idempotency-Key`, `deliveryType` y la combinación con `addressId`.
   2. Calcular el hash canónico.
   3. Buscar `(user_id, idempotency_key)` **antes de cargar o validar que el carrito no esté vacío**.
   4. Misma clave + mismo hash: devolver el pedido original, sin crear otro pedido, sin descontar stock y sin vaciar el carrito.
   5. Misma clave + distinto hash: responder `409 Conflict`.
   6. Clave nueva: cargar el carrito, validar que no esté vacío y continuar con el checkout normal.
   7. La primera request guarda `idempotency_key` y `checkout_request_hash` en la misma transacción que el pedido.
   8. Ante una carrera, la restricción única es la defensa final: capturar MySQL `1062`, releer el pedido existente y aplicar las reglas 4 o 5.
9. Consecuencia aceptada de excluir el carrito del hash: si el cliente reutiliza la misma clave con el mismo `deliveryType` y `addressId` pero con otro carrito, se devuelve el pedido original. El cliente debe usar una clave nueva para cada intención de checkout. No se agrega el carrito al hash porque así se decidió.
10. Los pedidos históricos quedan con `idempotency_key` y `checkout_request_hash` NULL; MySQL permite múltiples NULL en un índice único.
11. El código HTTP exacto del replay queda como detalle de implementación: se recomienda `200 OK` para replay y `201 Created` para pedido nuevo. No afecta el esquema.

### 3.2. Vencimiento

- `expires_at` lo calcula la **aplicación** como `created_at + 60 minutos`.
- El job de vencimiento busca `status = 'PENDING_PAYMENT' AND expires_at <= NOW(6)` usando `idx_orders_status_expires_at (status, expires_at)`.
- El pedido vencido pasa a `CANCELLED` con motivo `EXPIRED`; no se crea un estado nuevo.
- La restauración de stock debe ser única y transaccional: bloquear pedido y variantes, restaurar una sola vez y registrar el movimiento.
- No se usa columna generada ni default en la base.

### 3.3. Sin backfill histórico

No se completan datos de filas existentes:

- snapshots de dirección y de ítems;
- `order_id`, `order_item_id`, `resulting_stock`, `created_by_user_id`;
- `expires_at`, `idempotency_key`, `checkout_request_hash`;
- `cancelled_at` y `cancellation_reason`.

Los 17 movimientos existentes quedan con `order_id` y `order_item_id` NULL. No se infieren relaciones por motivo ni por fecha. Los pedidos 1 y 2 quedan con `cancelled_at`/`cancellation_reason` NULL.

### 3.4. `created_by_user_id`

- `NULL` representa al sistema (por ejemplo, vencimiento automático) o un movimiento histórico.
- Los movimientos ejecutados por un usuario deben guardar su id.
- No se crea usuario técnico `SYSTEM` en esta etapa.

### 3.5. Sin UNIQUE por ítem/tipo de movimiento

No se aplica `UNIQUE (order_item_id, stock_movement_type)`. Se elimina de la propuesta porque bloquearía cancelaciones parciales o más de un movimiento legítimo del mismo tipo por ítem.

### 3.6. Motivos de cancelación (D3)

Lista **acordada**, definida en `V2_06C` (cuya aplicación sigue siendo opcional):

```
EXPIRED, USER_CANCELLED, ADMIN_CANCELLED, PAYMENT_REJECTED, OTHER
```

La lista de motivos está aprobada; la aplicación del CHECK sigue siendo opcional. Si el negocio agrega motivos, editar `V2_06C` **antes** de aplicarlo. Una vez aplicado, un motivo fuera de la lista será rechazado. Si se prefiere no acoplar la base a los motivos, no aplicar `V2_06C`.

### 3.7. Trazabilidad compuesta de movimientos (D23)

- Se agrega `UNIQUE KEY uk_order_items_id_order (id, order_id)` en `order_items` como clave referenciada.
- Se agrega la FK compuesta `fk_stock_movements_order_item_order (order_item_id, order_id) -> order_items (id, order_id)`.
- Se elimina la FK independiente `order_item_id -> order_items(id)`: la compuesta la reemplaza y evita redundancia.
- Se conserva `fk_stock_movements_order (order_id) -> orders(id)` para movimientos asociados solo a un pedido.
- Se agrega el CHECK obligatorio `chk_stock_movements_order_item_requires_order (order_item_id IS NULL OR order_id IS NOT NULL)`. Es necesario porque InnoDB usa `MATCH SIMPLE`: una FK compuesta no se valida si alguna columna es NULL.
- Índice de la FK compuesta: `idx_stock_movements_order_item_order (order_item_id, order_id)`. Su prefijo cubre búsquedas por ítem, por lo que no se crea un índice separado sobre `order_item_id`.
- **Decisión cerrada:** se mantiene `UNIQUE (id, order_id)` para soportar la FK compuesta, aunque sea redundante con la PK.

### 3.8. Índices

Obligatorios:

- `uk_orders_user_idempotency (user_id, idempotency_key)`
- `idx_orders_status_expires_at (status, expires_at)`
- `uk_order_items_id_order (id, order_id)`
- `idx_stock_movements_order_id (order_id)`
- `idx_stock_movements_order_item_order (order_item_id, order_id)`
- `idx_stock_movements_created_by_user_id (created_by_user_id)`

No se incluyen índices opcionales: no hay una consulta de Etapa 2 que los requiera y los índices de FK ya cubren las búsquedas básicas.

### 3.9. CHECK obligatorios (D17)

- `product_variants.stock >= 0`.
- `order_items.quantity > 0` (más estricto que `<> 0`).
- `stock_movements.quantity <> 0` (permite negativos en ajustes).
- `stock_movements.order_item_id IS NULL OR stock_movements.order_id IS NOT NULL`.

**Decisión cerrada:** se mantiene el CHECK `order_items.quantity > 0`.

### 3.10. `resulting_stock` (D8)

- **Decisión cerrada:** lo calcula Java dentro de la misma transacción, después de bloquear la variante y aplicar el cambio de stock.
- No se propone trigger ni columna generada.
- La columna queda NULL para históricos.
- La implementación Java aún no se modificó en esta tarea.

### 3.11. `orders.status` (D11)

- **Decisión cerrada:** se mantiene `orders.status` nullable por ahora.
- Un pedido con `status` NULL no es tomado por el job de vencimiento; la app debe evitarlo en pedidos nuevos.
- No se agrega `NOT NULL` ni se cambia el ENUM en esta etapa.

### 3.12. DDL aditivo y ejecución única

- No se usa `IF NOT EXISTS`. Reejecutar un script falla por objeto duplicado; es intencional.
- Sin Flyway. Scripts manuales versionados con prefijo `V2_`.

---

## 4. Obligatorios vs. opcionales

### Obligatorios (V2_01 a V2_05)

Forman el camino mínimo. `V2_99` los verifica con un resumen que marca `FALTANTE` si falta un objeto o si su definición no coincide.

### Opcionales (V2_06A a V2_06D)

- Cada archivo contiene **un solo CHECK**.
- Se pueden aprobar y aplicar individualmente.
- Cada archivo incluye una prevalidación de solo lectura: si devuelve violaciones, **no aplicar**.
- La ausencia de un opcional **no es un error** en `V2_99`; se informa como `AUSENTE (opcional)`.
- `V2_06A`, `V2_06B` y `V2_06C` dependen de `V2_01`.
- `V2_06D` depende de `V2_04`.
- El rollback de cada opcional está en un bloque comentado independiente de `V2_90`; descomentar solo los que se aplicaron.

---

## 5. MySQL 8.0.46, CHECK y FK compuesta

- MySQL soporta y **aplica efectivamente** `CHECK` desde `8.0.16`. La versión verificada es `8.0.46`.
- Al agregar un `CHECK`, MySQL valida las filas existentes y falla si hay violaciones.
- Los `CHECK` se evalúan en `INSERT` y `UPDATE`.
- Un `CHECK` cuyo resultado es `NULL` se considera satisfecho. Por eso las validaciones relevantes usan columnas `NOT NULL` o `COALESCE`.
- MySQL no permite `CHECK` con subconsultas ni referencias a otras tablas.
- La FK compuesta requiere un índice sobre `(id, order_id)` en `order_items`; se usa un `UNIQUE` para compatibilidad futura.
- Con `MATCH SIMPLE`, una FK compuesta con algún NULL no se valida. El CHECK obligatorio cierra ese caso.
- Para demostrar la aplicación efectiva, usar `V2_98__opcional_test_en_clon.sql` **solo en un clon descartable**.

---

## 6. Procedimiento de aplicación (cuando se autorice)

1. Verificar conexión y base:
   ```bash
   mysql ... -D corralon_db -e "SELECT DATABASE(), VERSION();"
   ```
2. Generar backup lógico completo y verificar archivo, tamaño y cierre `Dump completed`. Ejemplo (adaptar credenciales; no ejecutado):
   ```bash
   mysqldump -h 127.0.0.1 -P 3307 -u"$DB_USER" -p"$DB_PASSWORD" \
     --default-character-set=utf8mb4 --single-transaction --quick --hex-blob \
     --no-tablespaces --databases corralon_db > corralon_db_etapa2_$(date +%Y%m%d_%H%M%S).sql
   ```
   **Para crear el clon de pruebas, NO usar `--databases`**: ese dump incluye `CREATE DATABASE`/`USE corralon_db` y al importarlo en el clon terminaría escribiendo en la base real. Para el clon, generar el dump sin `--databases` y con `corralon_db` como último argumento (ver `V2_98`).
3. Ejecutar `V2_00__prevalidacion.sql` y guardar la salida. **Detenerse** si:
   - hay colisiones de esquema;
   - alguna consulta bloqueante devuelve `violations > 0`;
   - la sección 2b devuelve `STOP` por `group_concat_max_len`;
   - la sección 3b devuelve `violations > 0` por tipos, nulabilidad o falta de PK/UNIQUE en las tablas referenciadas.
4. Ejecutar un archivo por vez, revisando errores:
   ```bash
   mysql ... -D corralon_db < V2_01__orders_checkout.sql
   mysql ... -D corralon_db < V2_02__orders_delivery_snapshot.sql
   mysql ... -D corralon_db < V2_03__order_items_snapshot.sql
   mysql ... -D corralon_db < V2_04__stock_movements_traceability.sql
   mysql ... -D corralon_db < V2_05__integrity_constraints.sql
   ```
5. Aplicar solo los opcionales aprobados, uno por archivo. Cada uno muestra su prevalidación antes del `ALTER`:
   ```bash
   mysql ... -D corralon_db < V2_06A__opt_orders_idempotency_pair.sql
   mysql ... -D corralon_db < V2_06B__opt_orders_cancelled_coherence.sql
   mysql ... -D corralon_db < V2_06C__opt_orders_cancellation_reason.sql
   mysql ... -D corralon_db < V2_06D__opt_stock_movements_resulting_stock.sql
   ```
6. Ejecutar `V2_99__postvalidacion.sql` y comparar:
   - resumen de obligatorios: todas las filas en `OK` (sin `FALTANTE`);
   - opcionales: `PRESENTE` o `AUSENTE (opcional)` según lo aprobado;
   - `row_count` y `fingerprint` idénticos a `V2_00`;
   - columnas nuevas en NULL para históricos;
   - revalidación de integridad en 0 (la fila informativa `7b`, pedidos `CANCELLED` históricos sin motivo, puede ser > 0 y no invalida la migración);
   - FK compuesta y coherencia `order_id`/`order_item_id` sin violaciones.
7. Antes de ejecutar la aplicación nueva, probar los CHECK y la FK compuesta en un clon descartable con `V2_98__opcional_test_en_clon.sql`.
8. Conservar el backup y las salidas de pre/postvalidación.

---

## 7. Rollback

`V2_90__rollback.sql` revierte el esquema en orden inverso:

```
opcionales aplicados -> V2_05 -> V2_04 -> V2_03 -> V2_02 -> V2_01
```

Cubre todos los objetos nuevos, incluidos:

- FK compuesta `fk_stock_movements_order_item_order`;
- CHECK `chk_stock_movements_order_item_requires_order`;
- índices `idx_stock_movements_order_item_order`, `idx_stock_movements_order_id`, `idx_stock_movements_created_by_user_id`;
- `uk_order_items_id_order` (se elimina **después** de la FK compuesta);
- checks opcionales, solo si se aplicaron (bloques comentados).

La verificación final de `V2_90` resume columnas, índices, FKs y CHECK; todas las filas deben quedar en `OK` con `remaining = 0`. La fila de checks opcionales también debe quedar en `OK` si se descomentaron los bloques correspondientes.

Limitaciones:

- MySQL hace **COMMIT implícito** en cada `ALTER`. No hay rollback transaccional de DDL.
- `DROP COLUMN` elimina definitivamente los datos escritos en esas columnas desde que se aplicó la migración.
- **El rollback no recupera datos** escritos después de aplicar los scripts: pedidos nuevos, movimientos nuevos, claves de idempotencia, snapshots, etc.
- Para volver al estado exacto con datos nuevos, restaurar el backup completo. El rollback de esquema es solo para estructura.
- No ejecutar sin backup verificado ni sin autorización.
- Descomentar únicamente los bloques opcionales que realmente se aplicaron.

---

## 8. Auditoría estática (2026-11-10)

### 8.1. Comprobaciones realizadas

- Se leyeron completos los 14 archivos: 13 SQL y este README.
- `V2_00` y `V2_99` usan exactamente las mismas seis huellas (`row_count` + `MD5(GROUP_CONCAT(...))`); se comparó el texto de ambas consultas y es idéntico.
- `V2_04` define `uk_order_items_id_order (id, order_id)`, la FK compuesta con columnas en el orden correcto, el índice `(order_item_id, order_id)` y el CHECK que cierra el caso `MATCH SIMPLE`.
- `V2_99` verifica la existencia de las 17 columnas (y muestra su definición para revisión), 6 índices, 3 FKs y 4 CHECK obligatorios; además valida la definición de la FK compuesta y de los índices compuestos. Los opcionales se informan aparte.
- `V2_99` separa la fila informativa de pedidos `CANCELLED` históricos sin motivo (`7b`): puede ser > 0 y no se mezcla con las validaciones bloqueantes.
- `V2_90` revierte las 17 columnas, 6 índices, 3 FKs y 4 CHECK obligatorios en orden inverso; los 4 CHECK opcionales tienen bloques independientes.
- Los 4 scripts opcionales son independientes entre sí, tienen prevalidación propia y bloque de rollback propio.
- `V2_98` tiene guardia bloqueante `USE corralon_db_checktest` + verificación de `DATABASE()`, advertencia destacada, instrucción de dump sin `--databases`, reemplazo de identificadores documentado, `ROLLBACK` por bloque y pruebas 1–9 (obligatorias y opcionales).
- No quedan nombres obsoletos (`idx_stock_movements_order_item_id`, `uk_stock_movements_order_item_type`, `chk_stock_movements_order_item_consistency`, `V2_06__optional_constraints`, etc.). La única mención a `fk_stock_movements_order_item` es un comentario que explica que no se crea.
- No hay referencias a scripts eliminados.

### 8.2. Correcciones aplicadas en esta auditoría

1. `V2_00`: se agregó prevalidación de tipo de `users.id` y de existencia de PK/UNIQUE en `orders.id`, `order_items.id` y `users.id`.
2. `V2_00`: `orders.status IS NULL` dejó de ser bloqueante y pasó a informativo, conforme a D11.
3. `V2_01`: se actualizaron los comentarios al contrato exacto de hash, alcance por usuario y orden de búsqueda.
4. `V2_06A`–`V2_06D`: se agregó prevalidación de solo lectura antes de cada `ALTER`.
5. `V2_06C`: la lista de motivos se documenta como acordada (D3), manteniendo la aplicación del CHECK como opcional.
6. `V2_99`: se agregó el resumen `4a` de objetos obligatorios con estado `OK`/`FALTANTE` y verificación de la definición de la FK compuesta y de `uk_order_items_id_order`.
7. `V2_90`: la verificación final ahora cubre todas las columnas, índices, FKs y CHECK (obligatorios y opcionales).
8. `V2_98`: se agregó guardia de clon y pruebas 7–9 para los CHECK opcionales.
9. `README`: se cerraron D3, D8, D11, D17, D21, D22 y D23, y se documentó el contrato de idempotencia.
10. `V2_99`: la fila de pedidos `CANCELLED` históricos sin motivo se movió a la sección informativa `7b`; ya no contradice la regla de que las validaciones de integridad deben dar 0.
11. `V2_98`: la guardia de clon pasó a ser bloqueante mediante `USE corralon_db_checktest`; se documentó que el dump del clon no debe usar `--databases` y se agregaron los opcionales `V2_06A`–`V2_06D` a la preparación.
12. `README`: se corrigió la redacción de D3, la verificación de `V2_99` y el procedimiento de backup/clon.

### 8.3. Decisiones cerradas

- **D3:** motivos `EXPIRED`, `USER_CANCELLED`, `ADMIN_CANCELLED`, `PAYMENT_REJECTED`, `OTHER`.
- **D8:** `resulting_stock` calculado por Java en la misma transacción.
- **D11:** `orders.status` se mantiene nullable.
- **D17:** se mantiene `order_items.quantity > 0`.
- **D21:** hash SHA-256 hexadecimal sobre `deliveryType` normalizado + `addressId` (o `null` para PICKUP), sin ítems; búsqueda de la clave antes de validar carrito vacío.
- **D22:** `Idempotency-Key` obligatoria en la API; columna nullable solo por historial.
- **D23:** se mantiene `UNIQUE (id, order_id)` para la FK compuesta.

### 8.4. Pendientes y riesgos abiertos

- **D12:** `ddl-auto: update` sigue activo. Al modificar entidades Java, Hibernate podría generar nombres distintos a los manuales. Recomendación: alinear `@Column`/`@Table` y evaluar `validate` en una etapa posterior.
- **D13:** medir locks de DDL en un clon con volumen real antes de producción.
- **D15:** validar zona horaria de `expires_at` en despliegue.
- **D16:** resolver concurrencia del job de vencimiento en Java (`SELECT ... FOR UPDATE`, opcionalmente `SKIP LOCKED`) para evitar doble restauración.
- **Detalle de implementación:** el código HTTP del replay queda a definir; se recomienda `200` para replay y `201` para creación. No afecta el esquema.
- **Riesgo aceptado de D21:** al excluir el carrito del hash, una clave reutilizada con el mismo `deliveryType`/`addressId` devuelve el pedido original aunque el carrito haya cambiado. El cliente debe usar una clave nueva por intención.

---

## 9. Estado de revisión por archivo

La revisión estática es completa. **Ningún archivo se marca como aprobado para ejecución**: eso requiere aprobación explícita, backup verificado y, para los CHECK/FK, prueba en clon.

| Archivo | Contenido revisado | Estado para ejecución |
|---|---|---|
| `V2_00__prevalidacion.sql` | Completo | No aprobado; solo lectura |
| `V2_01__orders_checkout.sql` | Completo | No aprobado |
| `V2_02__orders_delivery_snapshot.sql` | Completo | No aprobado |
| `V2_03__order_items_snapshot.sql` | Completo | No aprobado |
| `V2_04__stock_movements_traceability.sql` | Completo | No aprobado |
| `V2_05__integrity_constraints.sql` | Completo | No aprobado |
| `V2_06A__opt_orders_idempotency_pair.sql` | Completo | Opcional; no aprobado |
| `V2_06B__opt_orders_cancelled_coherence.sql` | Completo | Opcional; no aprobado |
| `V2_06C__opt_orders_cancellation_reason.sql` | Completo | Opcional; contenido D3 aprobado, aplicación no aprobada |
| `V2_06D__opt_stock_movements_resulting_stock.sql` | Completo | Opcional; no aprobado |
| `V2_90__rollback.sql` | Completo | No aprobado; solo contingencia |
| `V2_98__opcional_test_en_clon.sql` | Completo | No ejecutable tal cual; solo clon |
| `V2_99__postvalidacion.sql` | Completo | No aprobado; solo lectura |
| `README.md` | Completo | Documentación actualizada |

---

## 10. Lo que NO se hizo

- No se ejecutó ningún script.
- No se ejecutó ningún `ALTER`, `UPDATE`, `INSERT`, `DELETE`, `DROP` ni `TRUNCATE`.
- No se conectó a la base para modificar datos.
- No se modificó ningún archivo Java, YAML ni de configuración.
- No se cambió `ddl-auto`.
- No se creó ninguna migración Flyway.
- No se backfillearon datos históricos.
- No se relacionaron movimientos históricos con pedidos.
- No se ejecutó `V2_98`.
- No se asumió la aprobación de ningún script.
