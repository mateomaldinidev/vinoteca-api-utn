-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_01__orders_checkout.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL aditivo sobre orders
--
-- Requiere:
--   - V2_00__prevalidacion.sql sin bloqueos.
--   - Backup logico completo verificado.
--
-- Efecto:
--   - expires_at            : vencimiento del pedido (60 min, lo setea la app).
--   - cancellation_reason   : motivo de cancelacion (EXPIRED, etc.).
--   - cancelled_at          : fecha/hora efectiva de cancelacion.
--   - idempotency_key       : clave de idempotencia del checkout.
--   - checkout_request_hash : huella canonica del request de checkout.
--   - indice unico por usuario + idempotency_key.
--   - indice para el job de vencimiento (status, expires_at).
--
-- Decisiones aplicadas:
--   - Columnas NULL para no romper filas historicas.
--   - Idempotencia con alcance POR USUARIO:
--     UNIQUE (user_id, idempotency_key).
--   - idempotency_key: obligatoria para pedidos nuevos (la valida la API),
--     maximo 255 caracteres, se guarda tal cual la envia el cliente.
--     Collation binaria (comparacion case-sensitive).
--   - checkout_request_hash: SHA-256 HEXADECIMAL en minusculas (64
--     caracteres) del payload canonico de checkout (D21). La app escribe
--     solo hex; VARCHAR(128) ascii_bin deja margen sin cambiar el tipo.
--     Misma clave + mismo hash devuelve el pedido original; misma clave +
--     hash distinto responde 409 Conflict.
--   - La busqueda por (user_id, idempotency_key) debe ocurrir ANTES de
--     validar que el carrito no este vacio. El payload canonico contiene
--     solo deliveryType normalizado y addressId (UUID de la request, o null
--     para PICKUP); NO incluye items, precios ni totales.
--   - expires_at = created_at + 60 minutos, calculado por la APLICACION.
--     No se usa columna generada ni DEFAULT en la base.
--   - Sin backfill: las filas historicas quedan con NULL en todas estas
--     columnas. MySQL permite multiples NULL en el UNIQUE.
--
-- IMPORTANTE:
--   - Script de ejecucion UNICA. No usar IF NOT EXISTS: si se reejecuta,
--     MySQL fallara por columna/indice duplicado (comportamiento deseado).
--   - MySQL hace COMMIT implicito en cada ALTER. No hay rollback
--     transaccional de DDL; usar V2_90__rollback.sql si hiciera falta.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Columnas nuevas
-- ----------------------------------------------------------------------------
-- Se usa un ALTER por columna para que cada posicion AFTER apunte a una
-- columna que ya existe, sin depender del orden interno de un ALTER multiple.
ALTER TABLE orders
    ADD COLUMN expires_at DATETIME(6) NULL
        COMMENT 'Vencimiento del pedido. La app lo setea a created_at + 60 min.'
        AFTER created_at;

ALTER TABLE orders
    ADD COLUMN cancellation_reason VARCHAR(50) NULL
        COMMENT 'EXPIRED, USER_CANCELLED, ADMIN_CANCELLED, PAYMENT_REJECTED u otro motivo aprobado.'
        AFTER status;

ALTER TABLE orders
    ADD COLUMN cancelled_at DATETIME(6) NULL
        COMMENT 'Momento efectivo de la cancelacion.'
        AFTER cancellation_reason;

ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL
        COMMENT 'Idempotency-Key recibida en el checkout. NULL en pedidos historicos.'
        AFTER cancelled_at;

ALTER TABLE orders
    ADD COLUMN checkout_request_hash VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Hash canonico del request de checkout (ej. SHA-256 hex). NULL en pedidos historicos.'
        AFTER idempotency_key;

-- ----------------------------------------------------------------------------
-- 2. Restriccion unica de idempotencia
--    MySQL permite multiples NULL en indices UNIQUE, por lo que las filas
--    historicas (idempotency_key NULL) no colisionan entre si.
--    Alcance: por usuario. Si se decide alcance global, reemplazar por
--    UNIQUE KEY uk_orders_idempotency_key (idempotency_key).
-- ----------------------------------------------------------------------------
ALTER TABLE orders
    ADD UNIQUE KEY uk_orders_user_idempotency (user_id, idempotency_key);

-- ----------------------------------------------------------------------------
-- 3. Indice para el job de vencimiento
--    Query esperada:
--      SELECT ... FROM orders
--      WHERE status = 'PENDING_PAYMENT' AND expires_at <= NOW(6)
--      ORDER BY expires_at
-- ----------------------------------------------------------------------------
ALTER TABLE orders
    ADD KEY idx_orders_status_expires_at (status, expires_at);

-- ----------------------------------------------------------------------------
-- 4. Verificacion rapida local (opcional, solo lectura)
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND COLUMN_NAME IN (
      'expires_at','cancellation_reason','cancelled_at',
      'idempotency_key','checkout_request_hash')
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND INDEX_NAME IN ('uk_orders_user_idempotency','idx_orders_status_expires_at')
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
