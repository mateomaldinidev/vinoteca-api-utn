-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_90__rollback.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL de reversa
--
-- ############################################################################
-- ##  ADVERTENCIA: ESTE ARCHIVO NO RECUPERA DATOS.                         ##
-- ##  Revierte el ESQUEMA, no una transaccion. MySQL hace COMMIT implicito  ##
-- ##  en cada ALTER. Los datos escritos en las columnas nuevas (pedidos,    ##
-- ##  movimientos, idempotencia, snapshots) se PIERDEN al hacer DROP COLUMN. ##
-- ##  Antes de ejecutar: generar y verificar un backup completo.            ##
-- ##  Para volver al estado exacto con datos nuevos: restaurar el backup.   ##
-- ##  Ejecutar solo en entorno controlado y con autorizacion explicita.     ##
-- ############################################################################
--
-- Orden: inverso al de aplicacion.
--   Opcionales 06D -> 06C -> 06B -> 06A
--   -> V2_05 -> V2_04 -> V2_03 -> V2_02 -> V2_01
--
-- Los bloques opcionales estan COMENTADOS. Descomentar solo los scripts que
-- se hayan aplicado realmente. Si se aplico V2_06A..V2_06D completo,
-- descomentar los cuatro bloques.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- OPCIONAL 06D: solo si se aplico V2_06D__opt_stock_movements_resulting_stock.sql
-- ----------------------------------------------------------------------------
-- ALTER TABLE stock_movements DROP CHECK chk_stock_movements_resulting_stock_non_negative;

-- ----------------------------------------------------------------------------
-- OPCIONAL 06C: solo si se aplico V2_06C__opt_orders_cancellation_reason.sql
-- ----------------------------------------------------------------------------
-- ALTER TABLE orders DROP CHECK chk_orders_cancellation_reason;

-- ----------------------------------------------------------------------------
-- OPCIONAL 06B: solo si se aplico V2_06B__opt_orders_cancelled_coherence.sql
-- ----------------------------------------------------------------------------
-- ALTER TABLE orders DROP CHECK chk_orders_cancelled_at_status;

-- ----------------------------------------------------------------------------
-- OPCIONAL 06A: solo si se aplico V2_06A__opt_orders_idempotency_pair.sql
-- ----------------------------------------------------------------------------
-- ALTER TABLE orders DROP CHECK chk_orders_idempotency_pair;

-- ----------------------------------------------------------------------------
-- V2_05: CHECK obligatorios
-- ----------------------------------------------------------------------------
ALTER TABLE stock_movements  DROP CHECK chk_stock_movements_quantity_non_zero;
ALTER TABLE order_items      DROP CHECK chk_order_items_quantity_positive;
ALTER TABLE product_variants DROP CHECK chk_product_variants_stock_non_negative;

-- ----------------------------------------------------------------------------
-- V2_04: order_items / stock_movements
--   1) CHECK obligatorio antes de quitar las columnas que referencia.
--   2) FKs: primero la compuesta, luego las demas.
--   3) Indices de stock_movements.
--   4) UNIQUE referenciado por la FK compuesta (despues de eliminarla).
--   5) Columnas de stock_movements.
-- ----------------------------------------------------------------------------

-- 1) CHECK obligatorio
ALTER TABLE stock_movements
    DROP CHECK chk_stock_movements_order_item_requires_order;

-- 2) Foreign keys
ALTER TABLE stock_movements
    DROP FOREIGN KEY fk_stock_movements_created_by,
    DROP FOREIGN KEY fk_stock_movements_order_item_order,
    DROP FOREIGN KEY fk_stock_movements_order;

-- 3) Indices
ALTER TABLE stock_movements
    DROP INDEX idx_stock_movements_created_by_user_id,
    DROP INDEX idx_stock_movements_order_item_order,
    DROP INDEX idx_stock_movements_order_id;

-- 4) UNIQUE referenciado por la FK compuesta
--    Debe ejecutarse despues de eliminar fk_stock_movements_order_item_order.
ALTER TABLE order_items
    DROP INDEX uk_order_items_id_order;

-- 5) Columnas
ALTER TABLE stock_movements
    DROP COLUMN created_by_user_id,
    DROP COLUMN resulting_stock,
    DROP COLUMN order_item_id,
    DROP COLUMN order_id;

-- ----------------------------------------------------------------------------
-- V2_03: order_items (snapshots)
-- ----------------------------------------------------------------------------
ALTER TABLE order_items
    DROP COLUMN variant_attribute_snapshot,
    DROP COLUMN product_name_snapshot;

-- ----------------------------------------------------------------------------
-- V2_02: orders (snapshot de direccion)
-- ----------------------------------------------------------------------------
ALTER TABLE orders
    DROP COLUMN delivery_zip_code,
    DROP COLUMN delivery_city,
    DROP COLUMN delivery_apartment_number,
    DROP COLUMN delivery_floor,
    DROP COLUMN delivery_street_number,
    DROP COLUMN delivery_street;

-- ----------------------------------------------------------------------------
-- V2_01: orders (checkout / idempotencia / vencimiento)
-- ----------------------------------------------------------------------------
ALTER TABLE orders
    DROP INDEX idx_orders_status_expires_at,
    DROP INDEX uk_orders_user_idempotency;

ALTER TABLE orders
    DROP COLUMN checkout_request_hash,
    DROP COLUMN idempotency_key,
    DROP COLUMN cancelled_at,
    DROP COLUMN cancellation_reason,
    DROP COLUMN expires_at;

-- ----------------------------------------------------------------------------
-- Verificacion final (solo lectura): todos los objetos de la etapa deben
-- haber desaparecido. Cada fila debe devolver remaining = 0 y status = OK.
-- ----------------------------------------------------------------------------
SELECT 'columnas nuevas' AS object_group,
       COUNT(*) AS remaining, 0 AS expected,
       IF(COUNT(*) = 0, 'OK', 'REMANENTE') AS status
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
SELECT 'indices obligatorios', COUNT(DISTINCT INDEX_NAME), 0,
       IF(COUNT(DISTINCT INDEX_NAME) = 0, 'OK', 'REMANENTE')
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
SELECT 'foreign keys obligatorias', COUNT(DISTINCT CONSTRAINT_NAME), 0,
       IF(COUNT(DISTINCT CONSTRAINT_NAME) = 0, 'OK', 'REMANENTE')
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  AND CONSTRAINT_NAME IN (
      'fk_stock_movements_order',
      'fk_stock_movements_order_item_order',
      'fk_stock_movements_created_by')
UNION ALL
SELECT 'checks obligatorios', COUNT(DISTINCT CONSTRAINT_NAME), 0,
       IF(COUNT(DISTINCT CONSTRAINT_NAME) = 0, 'OK', 'REMANENTE')
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_TYPE = 'CHECK'
  AND CONSTRAINT_NAME IN (
      'chk_product_variants_stock_non_negative',
      'chk_order_items_quantity_positive',
      'chk_stock_movements_quantity_non_zero',
      'chk_stock_movements_order_item_requires_order')
UNION ALL
SELECT 'checks opcionales', COUNT(DISTINCT CONSTRAINT_NAME), 0,
       IF(COUNT(DISTINCT CONSTRAINT_NAME) = 0, 'OK',
          'REMANENTE: descomentar el bloque opcional aplicado')
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_TYPE = 'CHECK'
  AND CONSTRAINT_NAME IN (
      'chk_orders_idempotency_pair',
      'chk_orders_cancelled_at_status',
      'chk_orders_cancellation_reason',
      'chk_stock_movements_resulting_stock_non_negative')
ORDER BY object_group;
