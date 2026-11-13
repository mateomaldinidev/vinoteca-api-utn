-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_99__postvalidacion.sql
-- Version : 1.3 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : SOLO LECTURA
--
-- Objetivo:
--   1. Confirmar que las columnas, indices, FKs y CHECK existen.
--   2. Confirmar que las filas y huellas de datos existentes no cambiaron.
--   3. Confirmar que las columnas nuevas quedaron NULL en filas historicas
--      (salvo backfill aprobado).
--   4. Revalidar integridad.
--   5. Distinguir objetos OBLIGATORIOS (V2_01..V2_05) de OPCIONALES
--      (V2_06A..V2_06D): la ausencia de un opcional NO es un error.
--   6. Verificar la FK compuesta y la coherencia order_id/order_item_id.
--
-- Comparar la seccion "LINEA BASE" contra la salida de V2_00__prevalidacion.sql.
-- ============================================================================

SET SESSION group_concat_max_len = 1000000;

-- ----------------------------------------------------------------------------
-- 0. CONTEXTO
-- ----------------------------------------------------------------------------
SELECT DATABASE() AS current_database, VERSION() AS mysql_version, NOW(6) AS captured_at;

-- ----------------------------------------------------------------------------
-- 1. COLUMNAS NUEVAS (deben existir con estos tipos/nullabilidad)
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
        (TABLE_NAME = 'orders' AND COLUMN_NAME IN (
            'expires_at','cancellation_reason','cancelled_at',
            'idempotency_key','checkout_request_hash',
            'delivery_street','delivery_street_number','delivery_floor',
            'delivery_apartment_number','delivery_city','delivery_zip_code'))
     OR (TABLE_NAME = 'order_items' AND COLUMN_NAME IN (
            'product_name_snapshot','variant_attribute_snapshot'))
     OR (TABLE_NAME = 'stock_movements' AND COLUMN_NAME IN (
            'order_id','order_item_id','resulting_stock','created_by_user_id'))
      )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- ----------------------------------------------------------------------------
-- 2. INDICES OBLIGATORIOS (V2_01..V2_05)
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN (
      'uk_orders_user_idempotency',
      'idx_orders_status_expires_at',
      'uk_order_items_id_order',
      'idx_stock_movements_order_id',
      'idx_stock_movements_order_item_order',
      'idx_stock_movements_created_by_user_id'
  )
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ----------------------------------------------------------------------------
-- 3. FOREIGN KEYS OBLIGATORIAS (V2_04)
--    ORDINAL_POSITION muestra las dos columnas de la FK compuesta.
-- ----------------------------------------------------------------------------
SELECT CONSTRAINT_NAME, ORDINAL_POSITION, TABLE_NAME, COLUMN_NAME,
       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME IN (
      'fk_stock_movements_order',
      'fk_stock_movements_order_item_order',
      'fk_stock_movements_created_by')
ORDER BY CONSTRAINT_NAME, ORDINAL_POSITION;

-- ----------------------------------------------------------------------------
-- 4. CHECK OBLIGATORIOS (V2_04 y V2_05)
-- ----------------------------------------------------------------------------
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME IN (
      'chk_product_variants_stock_non_negative',
      'chk_order_items_quantity_positive',
      'chk_stock_movements_quantity_non_zero',
      'chk_stock_movements_order_item_requires_order')
ORDER BY CONSTRAINT_NAME;

-- ----------------------------------------------------------------------------
-- 4a. RESUMEN DE OBJETOS OBLIGATORIOS (V2_01..V2_05)
--     Ausencia o definicion incompleta = error. Los opcionales se informan
--     por separado en 4b y su ausencia NO es un error.
-- ----------------------------------------------------------------------------
SELECT 'columnas nuevas' AS object_group,
       COUNT(*) AS found, 17 AS expected,
       IF(COUNT(*) = 17, 'OK', 'FALTANTE') AS status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
        (TABLE_NAME = 'orders' AND COLUMN_NAME IN (
            'expires_at','cancellation_reason','cancelled_at',
            'idempotency_key','checkout_request_hash',
            'delivery_street','delivery_street_number','delivery_floor',
            'delivery_apartment_number','delivery_city','delivery_zip_code'))
     OR (TABLE_NAME = 'order_items' AND COLUMN_NAME IN (
            'product_name_snapshot','variant_attribute_snapshot'))
     OR (TABLE_NAME = 'stock_movements' AND COLUMN_NAME IN (
            'order_id','order_item_id','resulting_stock','created_by_user_id'))
      )
UNION ALL
SELECT 'indices obligatorios', COUNT(DISTINCT INDEX_NAME), 6,
       IF(COUNT(DISTINCT INDEX_NAME) = 6, 'OK', 'FALTANTE')
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN (
      'uk_orders_user_idempotency',
      'idx_orders_status_expires_at',
      'uk_order_items_id_order',
      'idx_stock_movements_order_id',
      'idx_stock_movements_order_item_order',
      'idx_stock_movements_created_by_user_id')
UNION ALL
SELECT 'foreign keys obligatorias', COUNT(DISTINCT CONSTRAINT_NAME), 3,
       IF(COUNT(DISTINCT CONSTRAINT_NAME) = 3, 'OK', 'FALTANTE')
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  AND CONSTRAINT_NAME IN (
      'fk_stock_movements_order',
      'fk_stock_movements_order_item_order',
      'fk_stock_movements_created_by')
UNION ALL
SELECT 'checks obligatorios', COUNT(DISTINCT CONSTRAINT_NAME), 4,
       IF(COUNT(DISTINCT CONSTRAINT_NAME) = 4, 'OK', 'FALTANTE')
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_TYPE = 'CHECK'
  AND CONSTRAINT_NAME IN (
      'chk_product_variants_stock_non_negative',
      'chk_order_items_quantity_positive',
      'chk_stock_movements_quantity_non_zero',
      'chk_stock_movements_order_item_requires_order')
UNION ALL
SELECT 'FK compuesta: columnas', COUNT(*), 2,
       IF(COUNT(*) = 2, 'OK', 'FALTANTE')
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'fk_stock_movements_order_item_order'
UNION ALL
SELECT 'FK compuesta: referencias', COUNT(*), 2,
       IF(COUNT(*) = 2, 'OK', 'FALTANTE')
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'fk_stock_movements_order_item_order'
  AND REFERENCED_TABLE_NAME = 'order_items'
  AND (
        (ORDINAL_POSITION = 1 AND COLUMN_NAME = 'order_item_id'
            AND REFERENCED_COLUMN_NAME = 'id')
     OR (ORDINAL_POSITION = 2 AND COLUMN_NAME = 'order_id'
            AND REFERENCED_COLUMN_NAME = 'order_id')
      )
UNION ALL
SELECT 'uk_order_items_id_order: definicion', COUNT(*), 2,
       IF(COUNT(*) = 2, 'OK', 'FALTANTE')
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_items'
  AND INDEX_NAME = 'uk_order_items_id_order'
  AND NON_UNIQUE = 0
  AND (
        (SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'id')
     OR (SEQ_IN_INDEX = 2 AND COLUMN_NAME = 'order_id')
      )
UNION ALL
SELECT 'idx_stock_movements_order_item_order: definicion', COUNT(*), 2,
       IF(COUNT(*) = 2, 'OK', 'FALTANTE')
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'stock_movements'
  AND INDEX_NAME = 'idx_stock_movements_order_item_order'
  AND (
        (SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'order_item_id')
     OR (SEQ_IN_INDEX = 2 AND COLUMN_NAME = 'order_id')
      )
ORDER BY object_group;

-- ----------------------------------------------------------------------------
-- 4b. OBJETOS OPCIONALES (V2_06A..V2_06D)
--     La ausencia NO es un error. Esta seccion solo informa si se aplicaron.
-- ----------------------------------------------------------------------------
SELECT 'chk_orders_idempotency_pair' AS object_name,
       IF(COUNT(*) > 0, 'PRESENTE', 'AUSENTE (opcional)') AS status
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_orders_idempotency_pair'
UNION ALL
SELECT 'chk_orders_cancelled_at_status',
       IF(COUNT(*) > 0, 'PRESENTE', 'AUSENTE (opcional)')
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_orders_cancelled_at_status'
UNION ALL
SELECT 'chk_orders_cancellation_reason',
       IF(COUNT(*) > 0, 'PRESENTE', 'AUSENTE (opcional)')
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_orders_cancellation_reason'
UNION ALL
SELECT 'chk_stock_movements_resulting_stock_non_negative',
       IF(COUNT(*) > 0, 'PRESENTE', 'AUSENTE (opcional)')
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_stock_movements_resulting_stock_non_negative';

-- ----------------------------------------------------------------------------
-- 5. LINEA BASE: filas y huellas (deben coincidir con V2_00)
-- ----------------------------------------------------------------------------
SELECT 'orders' AS table_name, COUNT(*) AS row_count,
       MD5(GROUP_CONCAT(CONCAT_WS('|',
           id,
           DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s.%f'),
           delivery_type,
           HEX(external_id),
           COALESCE(status, '<NULL>'),
           total,
           COALESCE(CAST(address_id AS CHAR), '<NULL>'),
           user_id
       ) ORDER BY id SEPARATOR ';')) AS fingerprint
FROM orders
UNION ALL
SELECT 'order_items', COUNT(*),
       MD5(GROUP_CONCAT(CONCAT_WS('|',
           id, HEX(external_id), order_id, product_variant_id,
           quantity, unit_price, subtotal
       ) ORDER BY id SEPARATOR ';'))
FROM order_items
UNION ALL
SELECT 'stock_movements', COUNT(*),
       MD5(GROUP_CONCAT(CONCAT_WS('|',
           id, HEX(external_id),
           DATE_FORMAT(movement_date, '%Y-%m-%d %H:%i:%s.%f'),
           quantity, COALESCE(reason, '<NULL>'),
           stock_movement_type, variant_id
       ) ORDER BY id SEPARATOR ';'))
FROM stock_movements
UNION ALL
SELECT 'product_variants', COUNT(*),
       MD5(GROUP_CONCAT(CONCAT_WS('|',
           id, active + 0, attribute, HEX(external_id), price, stock,
           COALESCE(CAST(wholesale_min_qty AS CHAR), '<NULL>'),
           COALESCE(CAST(wholesale_price AS CHAR), '<NULL>'),
           product_id
       ) ORDER BY id SEPARATOR ';'))
FROM product_variants
UNION ALL
SELECT 'addresses', COUNT(*),
       MD5(GROUP_CONCAT(CONCAT_WS('|',
           id, active + 0,
           COALESCE(apartment_number, '<NULL>'),
           city, HEX(external_id),
           COALESCE(floor, '<NULL>'),
           street, street_number, zip_code, user_id
       ) ORDER BY id SEPARATOR ';'))
FROM addresses
UNION ALL
SELECT 'payments', COUNT(*),
       MD5(GROUP_CONCAT(CONCAT_WS('|',
           id, amount,
           DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s.%f'),
           HEX(external_id), payment_method, status, order_id
       ) ORDER BY id SEPARATOR ';'))
FROM payments;

-- ----------------------------------------------------------------------------
-- 6. COLUMNAS NUEVAS: cuantas quedaron NULL (historicos)
--    Sin backfill aprobado, todos los contadores deben igualar el total.
-- ----------------------------------------------------------------------------
SELECT
    COUNT(*)                              AS orders_total,
    SUM(expires_at IS NULL)               AS expires_at_null,
    SUM(cancellation_reason IS NULL)      AS cancellation_reason_null,
    SUM(cancelled_at IS NULL)             AS cancelled_at_null,
    SUM(idempotency_key IS NULL)          AS idempotency_key_null,
    SUM(checkout_request_hash IS NULL)    AS checkout_request_hash_null,
    SUM(delivery_street IS NULL)          AS delivery_street_null,
    SUM(delivery_street_number IS NULL)   AS delivery_street_number_null,
    SUM(delivery_floor IS NULL)           AS delivery_floor_null,
    SUM(delivery_apartment_number IS NULL) AS delivery_apartment_number_null,
    SUM(delivery_city IS NULL)            AS delivery_city_null,
    SUM(delivery_zip_code IS NULL)        AS delivery_zip_code_null
FROM orders;

SELECT
    COUNT(*)                                  AS order_items_total,
    SUM(product_name_snapshot IS NULL)        AS product_name_snapshot_null,
    SUM(variant_attribute_snapshot IS NULL)   AS variant_attribute_snapshot_null
FROM order_items;

SELECT
    COUNT(*)                          AS stock_movements_total,
    SUM(order_id IS NULL)             AS order_id_null,
    SUM(order_item_id IS NULL)        AS order_item_id_null,
    SUM(resulting_stock IS NULL)      AS resulting_stock_null,
    SUM(created_by_user_id IS NULL)   AS created_by_user_id_null
FROM stock_movements;

-- ----------------------------------------------------------------------------
-- 7. REVALIDACION DE INTEGRIDAD (todas las filas bloqueantes deben dar 0)
-- ----------------------------------------------------------------------------
SELECT 'product_variants.stock < 0'                 AS check_name, COUNT(*) AS violations FROM product_variants WHERE stock < 0
UNION ALL
SELECT 'order_items.quantity <= 0',                 COUNT(*) FROM order_items WHERE quantity <= 0
UNION ALL
SELECT 'stock_movements.quantity = 0',              COUNT(*) FROM stock_movements WHERE quantity = 0
UNION ALL
SELECT 'resulting_stock < 0',                       COUNT(*) FROM stock_movements WHERE resulting_stock < 0
UNION ALL
SELECT 'order_item_id sin order_id',                COUNT(*) FROM stock_movements WHERE order_item_id IS NOT NULL AND order_id IS NULL
UNION ALL
SELECT 'order_id huerfano',                         COUNT(*)
    FROM stock_movements sm LEFT JOIN orders o ON o.id = sm.order_id
    WHERE sm.order_id IS NOT NULL AND o.id IS NULL
UNION ALL
SELECT 'order_item_id huerfano',                    COUNT(*)
    FROM stock_movements sm LEFT JOIN order_items oi ON oi.id = sm.order_item_id
    WHERE sm.order_item_id IS NOT NULL AND oi.id IS NULL
UNION ALL
SELECT 'created_by_user_id huerfano',               COUNT(*)
    FROM stock_movements sm LEFT JOIN users u ON u.id = sm.created_by_user_id
    WHERE sm.created_by_user_id IS NOT NULL AND u.id IS NULL
UNION ALL
SELECT 'idempotency_key duplicada por usuario',     COUNT(*)
    FROM (SELECT user_id, idempotency_key
          FROM orders
          WHERE idempotency_key IS NOT NULL
          GROUP BY user_id, idempotency_key
          HAVING COUNT(*) > 1) d
UNION ALL
SELECT 'order_item no pertenece al order_id',       COUNT(*)
    FROM stock_movements sm
    JOIN order_items oi ON oi.id = sm.order_item_id
    WHERE sm.order_id IS NOT NULL AND oi.order_id <> sm.order_id;

-- ----------------------------------------------------------------------------
-- 7b. INFORMATIVO (NO bloqueante)
--     Pedidos CANCELLED historicos sin motivo. Puede dar > 0: la columna
--     cancellation_reason es nueva y no se backfillea (D3/V2_06C aplican a
--     cancelaciones nuevas). No invalida la migracion.
-- ----------------------------------------------------------------------------
SELECT 'orders CANCELLED sin motivo (informativo)' AS check_name,
       COUNT(*) AS value
FROM orders
WHERE status = 'CANCELLED' AND cancellation_reason IS NULL;

-- ----------------------------------------------------------------------------
-- 8. ESTRUCTURA FINAL PARA REVISION VISUAL
-- ----------------------------------------------------------------------------
SHOW CREATE TABLE orders;
SHOW CREATE TABLE order_items;
SHOW CREATE TABLE stock_movements;
