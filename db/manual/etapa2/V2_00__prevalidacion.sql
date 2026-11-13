-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_00__prevalidacion.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : SOLO LECTURA (no modifica estructura ni datos)
--
-- Objetivo:
--   1. Confirmar base, version y motor.
--   2. Confirmar que no existen columnas/indices/constraints con los nombres
--      que se van a crear.
--   3. Registrar una linea base de filas y huellas (fingerprints) para
--      comparar despues de aplicar el DDL.
--   4. Detectar datos incompatibles con las restricciones propuestas.
--   5. Detectar truncamiento silencioso de GROUP_CONCAT en las huellas.
--   6. Prevalidar tipos y unicidad requeridos por la FK compuesta
--      (order_item_id, order_id) -> order_items (id, order_id).
--
-- Como usar:
--   mysql -h ... -P ... -u... -p ... -D corralon_db < V2_00__prevalidacion.sql
--   Guardar la salida en un archivo para compararla con V2_99.
--
-- Criterio de corte:
--   - Cualquier consulta de la seccion "CHECKS BLOQUEANTES" con
--     violations > 0 => DETENERSE y no ejecutar los scripts 01..05.
--   - Cualquier fila en "COLISIONES DE ESQUEMA" => DETENERSE.
--   - version_num < 80016 => DETENERSE (CHECK no se aplica antes de 8.0.16).
--   - Cualquier status 'STOP' en la seccion 2b => DETENERSE y aumentar
--     group_concat_max_len antes de confiar en las huellas.
-- ============================================================================

SET SESSION group_concat_max_len = 1000000;

-- ----------------------------------------------------------------------------
-- 0. CONTEXTO
-- ----------------------------------------------------------------------------
SELECT
    DATABASE()                AS current_database,
    VERSION()                 AS mysql_version,
    @@version_comment         AS version_comment,
    @@sql_mode                AS sql_mode,
    NOW(6)                    AS captured_at;

SELECT
    CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) * 10000
  + CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) * 100
  + CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 3), '.', -1) AS UNSIGNED) AS version_num,
    IF(
        CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) * 10000
      + CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) * 100
      + CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 3), '.', -1) AS UNSIGNED) >= 80016,
        'OK',
        'STOP: se requiere MySQL >= 8.0.16 para CHECK'
    ) AS check_mysql_check_support;

SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('orders','order_items','stock_movements','product_variants','addresses','payments','users','products')
ORDER BY TABLE_NAME;

-- ----------------------------------------------------------------------------
-- 1. COLISIONES DE ESQUEMA (debe devolver 0 filas)
-- ----------------------------------------------------------------------------
SELECT 'COLISION_COLUMNA' AS collision_type, TABLE_NAME, COLUMN_NAME AS name
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
      );

SELECT 'COLISION_INDICE' AS collision_type, TABLE_NAME, INDEX_NAME AS name
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
GROUP BY TABLE_NAME, INDEX_NAME;

SELECT 'COLISION_CONSTRAINT' AS collision_type, TABLE_NAME, CONSTRAINT_NAME AS name
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME IN (
      'fk_stock_movements_order',
      'fk_stock_movements_order_item_order',
      'fk_stock_movements_created_by',
      'chk_product_variants_stock_non_negative',
      'chk_order_items_quantity_positive',
      'chk_stock_movements_quantity_non_zero',
      'chk_stock_movements_order_item_requires_order',
      'chk_stock_movements_resulting_stock_non_negative',
      'chk_orders_cancellation_reason',
      'chk_orders_idempotency_pair',
      'chk_orders_cancelled_at_status'
  );

-- ----------------------------------------------------------------------------
-- 2. LINEA BASE DE FILAS Y HUELLAS
--    Comparar exactamente contra la salida de V2_99__postvalidacion.sql.
--    Las huellas usan SOLO las columnas existentes antes de la Etapa 2.
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
-- 2b. CONTROL DE TRUNCAMIENTO DE GROUP_CONCAT
--     Las huellas de la seccion 2 se truncan en silencio si el texto supera
--     @@session.group_concat_max_len (que se mide en BYTES). Esta seccion
--     estima el largo exacto del texto concatenado y marca STOP si no entra.
--     Si aparece STOP: aumentar group_concat_max_len y volver a ejecutar
--     V2_00 antes de confiar en las huellas.
-- ----------------------------------------------------------------------------
SELECT 'orders' AS table_name, COUNT(*) AS rows_total,
       CAST(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) AS UNSIGNED) AS estimated_bytes,
       @@SESSION.group_concat_max_len AS concat_limit,
       IF(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) <= @@SESSION.group_concat_max_len,
          'OK', 'STOP: aumentar group_concat_max_len') AS status
FROM (
    SELECT LENGTH(CONCAT_WS('|',
        id,
        DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s.%f'),
        delivery_type,
        HEX(external_id),
        COALESCE(status, '<NULL>'),
        total,
        COALESCE(CAST(address_id AS CHAR), '<NULL>'),
        user_id
    )) AS len
    FROM orders
) t
UNION ALL
SELECT 'order_items', COUNT(*),
       CAST(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) AS UNSIGNED),
       @@SESSION.group_concat_max_len,
       IF(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) <= @@SESSION.group_concat_max_len,
          'OK', 'STOP: aumentar group_concat_max_len')
FROM (
    SELECT LENGTH(CONCAT_WS('|',
        id, HEX(external_id), order_id, product_variant_id,
        quantity, unit_price, subtotal
    )) AS len
    FROM order_items
) t
UNION ALL
SELECT 'stock_movements', COUNT(*),
       CAST(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) AS UNSIGNED),
       @@SESSION.group_concat_max_len,
       IF(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) <= @@SESSION.group_concat_max_len,
          'OK', 'STOP: aumentar group_concat_max_len')
FROM (
    SELECT LENGTH(CONCAT_WS('|',
        id, HEX(external_id),
        DATE_FORMAT(movement_date, '%Y-%m-%d %H:%i:%s.%f'),
        quantity, COALESCE(reason, '<NULL>'),
        stock_movement_type, variant_id
    )) AS len
    FROM stock_movements
) t
UNION ALL
SELECT 'product_variants', COUNT(*),
       CAST(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) AS UNSIGNED),
       @@SESSION.group_concat_max_len,
       IF(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) <= @@SESSION.group_concat_max_len,
          'OK', 'STOP: aumentar group_concat_max_len')
FROM (
    SELECT LENGTH(CONCAT_WS('|',
        id, active + 0, attribute, HEX(external_id), price, stock,
        COALESCE(CAST(wholesale_min_qty AS CHAR), '<NULL>'),
        COALESCE(CAST(wholesale_price AS CHAR), '<NULL>'),
        product_id
    )) AS len
    FROM product_variants
) t
UNION ALL
SELECT 'addresses', COUNT(*),
       CAST(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) AS UNSIGNED),
       @@SESSION.group_concat_max_len,
       IF(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) <= @@SESSION.group_concat_max_len,
          'OK', 'STOP: aumentar group_concat_max_len')
FROM (
    SELECT LENGTH(CONCAT_WS('|',
        id, active + 0,
        COALESCE(apartment_number, '<NULL>'),
        city, HEX(external_id),
        COALESCE(floor, '<NULL>'),
        street, street_number, zip_code, user_id
    )) AS len
    FROM addresses
) t
UNION ALL
SELECT 'payments', COUNT(*),
       CAST(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) AS UNSIGNED),
       @@SESSION.group_concat_max_len,
       IF(COALESCE(SUM(len), 0) + GREATEST(COUNT(*) - 1, 0) <= @@SESSION.group_concat_max_len,
          'OK', 'STOP: aumentar group_concat_max_len')
FROM (
    SELECT LENGTH(CONCAT_WS('|',
        id, amount,
        DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s.%f'),
        HEX(external_id), payment_method, status, order_id
    )) AS len
    FROM payments
) t;

-- ----------------------------------------------------------------------------
-- 3. CHECKS BLOQUEANTES (todas deben devolver violations = 0)
-- ----------------------------------------------------------------------------
SELECT 'product_variants.stock < 0'                      AS check_name, COUNT(*) AS violations, 0 AS expected FROM product_variants WHERE stock < 0
UNION ALL
SELECT 'product_variants.stock IS NULL',                 COUNT(*), 0 FROM product_variants WHERE stock IS NULL
UNION ALL
SELECT 'order_items.quantity <= 0',                      COUNT(*), 0 FROM order_items WHERE quantity <= 0
UNION ALL
SELECT 'order_items.quantity IS NULL',                   COUNT(*), 0 FROM order_items WHERE quantity IS NULL
UNION ALL
SELECT 'stock_movements.quantity = 0',                   COUNT(*), 0 FROM stock_movements WHERE quantity = 0
UNION ALL
SELECT 'stock_movements.quantity IS NULL',               COUNT(*), 0 FROM stock_movements WHERE quantity IS NULL
UNION ALL
SELECT 'orders.total < 0',                               COUNT(*), 0 FROM orders WHERE total < 0
UNION ALL
SELECT 'order_items.unit_price < 0',                     COUNT(*), 0 FROM order_items WHERE unit_price < 0
UNION ALL
SELECT 'order_items.subtotal < 0',                       COUNT(*), 0 FROM order_items WHERE subtotal < 0
UNION ALL
SELECT 'orders DELIVERY sin address_id',                 COUNT(*), 0 FROM orders WHERE delivery_type = 'DELIVERY' AND address_id IS NULL
UNION ALL
SELECT 'orders PICKUP con address_id',                   COUNT(*), 0 FROM orders WHERE delivery_type = 'PICKUP' AND address_id IS NOT NULL
UNION ALL
SELECT 'orders.address_id huerfano',                     COUNT(*), 0
    FROM orders o LEFT JOIN addresses a ON a.id = o.address_id
    WHERE o.address_id IS NOT NULL AND a.id IS NULL
UNION ALL
SELECT 'orders.user_id huerfano',                        COUNT(*), 0
    FROM orders o LEFT JOIN users u ON u.id = o.user_id
    WHERE u.id IS NULL
UNION ALL
SELECT 'order_items.order_id huerfano',                  COUNT(*), 0
    FROM order_items oi LEFT JOIN orders o ON o.id = oi.order_id
    WHERE o.id IS NULL
UNION ALL
SELECT 'order_items.product_variant_id huerfano',        COUNT(*), 0
    FROM order_items oi LEFT JOIN product_variants pv ON pv.id = oi.product_variant_id
    WHERE pv.id IS NULL
UNION ALL
SELECT 'stock_movements.variant_id huerfano',            COUNT(*), 0
    FROM stock_movements sm LEFT JOIN product_variants pv ON pv.id = sm.variant_id
    WHERE pv.id IS NULL
UNION ALL
SELECT 'addresses campos obligatorios nulos/vacios',     COUNT(*), 0
    FROM addresses
    WHERE street IS NULL OR TRIM(street) = ''
       OR street_number IS NULL OR TRIM(street_number) = ''
       OR city IS NULL OR TRIM(city) = ''
       OR zip_code IS NULL OR TRIM(zip_code) = ''
UNION ALL
SELECT 'products.name nulo/vacio',                       COUNT(*), 0
    FROM products WHERE name IS NULL OR TRIM(name) = ''
UNION ALL
SELECT 'product_variants.attribute nulo/vacio',          COUNT(*), 0
    FROM product_variants WHERE attribute IS NULL OR TRIM(attribute) = ''
UNION ALL
SELECT 'order_items duplicado (order_id, variant_id)',   COUNT(*), 0
    FROM (SELECT order_id, product_variant_id
          FROM order_items
          GROUP BY order_id, product_variant_id
          HAVING COUNT(*) > 1) d
UNION ALL
SELECT 'orders.external_id duplicado',                   COUNT(*), 0
    FROM (SELECT external_id FROM orders GROUP BY external_id HAVING COUNT(*) > 1) d;

-- ----------------------------------------------------------------------------
-- 3b. PREVALIDACION DE LA FK COMPUESTA (information_schema)
--     La FK compuesta (order_item_id, order_id) -> order_items(id, order_id)
--     requiere:
--       - order_items.order_id NOT NULL y de tipo compatible;
--       - orders.id y order_items.id del mismo tipo (BIGINT);
--       - unicidad de (id, order_id), que aporta el UNIQUE de V2_04.
--     Los movimientos existentes quedan con order_id/order_item_id NULL,
--     por lo que la FK no encuentra filas invalidas al crearse.
-- ----------------------------------------------------------------------------
SELECT 'FK compuesta: order_items.order_id debe ser BIGINT NOT NULL' AS check_name,
       COUNT(*) AS violations, 0 AS expected
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_items'
  AND COLUMN_NAME = 'order_id'
  AND (IS_NULLABLE <> 'NO' OR COLUMN_TYPE <> 'bigint')
UNION ALL
SELECT 'FK compuesta: orders.id debe ser BIGINT', COUNT(*), 0
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND COLUMN_NAME = 'id'
  AND COLUMN_TYPE <> 'bigint'
UNION ALL
SELECT 'FK compuesta: order_items.id debe ser BIGINT', COUNT(*), 0
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_items'
  AND COLUMN_NAME = 'id'
  AND COLUMN_TYPE <> 'bigint'
UNION ALL
SELECT 'FK usuario: users.id debe ser BIGINT', COUNT(*), 0
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME = 'id'
  AND COLUMN_TYPE <> 'bigint'
UNION ALL
SELECT 'FK compuesta: orders.id debe ser PK/UNIQUE', IF(COUNT(*) > 0, 0, 1), 0
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND COLUMN_NAME = 'id'
  AND NON_UNIQUE = 0
  AND SEQ_IN_INDEX = 1
UNION ALL
SELECT 'FK compuesta: order_items.id debe ser PK/UNIQUE', IF(COUNT(*) > 0, 0, 1), 0
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_items'
  AND COLUMN_NAME = 'id'
  AND NON_UNIQUE = 0
  AND SEQ_IN_INDEX = 1
UNION ALL
SELECT 'FK usuario: users.id debe ser PK/UNIQUE', IF(COUNT(*) > 0, 0, 1), 0
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME = 'id'
  AND NON_UNIQUE = 0
  AND SEQ_IN_INDEX = 1
UNION ALL
SELECT 'FK compuesta: (id, order_id) sin duplicados', COUNT(*), 0
FROM (SELECT id, order_id
      FROM order_items
      GROUP BY id, order_id
      HAVING COUNT(*) > 1) d;

-- ----------------------------------------------------------------------------
-- 4. CHECKS INFORMATIVOS (no bloquean por si solos; registrar la salida)
-- ----------------------------------------------------------------------------
-- D11 aprobada: orders.status se mantiene nullable. Un status NULL no es
-- un bloqueo de esta migracion; se informa para que la app no lo genere.
SELECT 'orders.status NULL (informativo, D11)' AS metric, COUNT(*) AS value
FROM orders WHERE status IS NULL;

SELECT status, COUNT(*) AS n FROM orders GROUP BY status ORDER BY status;

SELECT stock_movement_type, COUNT(*) AS n, MIN(quantity) AS min_qty, MAX(quantity) AS max_qty
FROM stock_movements GROUP BY stock_movement_type ORDER BY stock_movement_type;

SELECT 'orders PENDING_PAYMENT' AS metric, COUNT(*) AS value FROM orders WHERE status = 'PENDING_PAYMENT'
UNION ALL
SELECT 'orders CANCELLED', COUNT(*) FROM orders WHERE status = 'CANCELLED'
UNION ALL
SELECT 'orders sin address_id', COUNT(*) FROM orders WHERE address_id IS NULL
UNION ALL
SELECT 'orders con address_id', COUNT(*) FROM orders WHERE address_id IS NOT NULL
UNION ALL
SELECT 'orders address inactiva', COUNT(*) FROM orders o JOIN addresses a ON a.id = o.address_id WHERE a.active = 0;

SELECT 'products.name max len' AS metric, MAX(CHAR_LENGTH(name)) AS value FROM products
UNION ALL
SELECT 'product_variants.attribute max len', MAX(CHAR_LENGTH(attribute)) FROM product_variants
UNION ALL
SELECT 'stock_movements.reason max len', MAX(CHAR_LENGTH(reason)) FROM stock_movements;

-- Longitudes propuestas: product_name_snapshot VARCHAR(255),
-- variant_attribute_snapshot VARCHAR(255), cancellation_reason VARCHAR(50).
-- Verificar que los maximos actuales entren en esos limites.

-- ----------------------------------------------------------------------------
-- 5. RESUMEN DE COLUMNAS EXISTENTES QUE SE VAN A USAR COMO ORIGEN
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
        (TABLE_NAME = 'addresses' AND COLUMN_NAME IN (
            'id','street','street_number','floor','apartment_number','city','zip_code'))
     OR (TABLE_NAME = 'products' AND COLUMN_NAME IN ('id','name'))
     OR (TABLE_NAME = 'product_variants' AND COLUMN_NAME IN ('id','attribute','stock'))
     OR (TABLE_NAME = 'orders' AND COLUMN_NAME IN ('id','status','created_at','address_id','user_id'))
     OR (TABLE_NAME = 'order_items' AND COLUMN_NAME IN ('id','order_id','product_variant_id','quantity'))
     OR (TABLE_NAME = 'users' AND COLUMN_NAME IN ('id','email'))
      )
ORDER BY TABLE_NAME, ORDINAL_POSITION;
