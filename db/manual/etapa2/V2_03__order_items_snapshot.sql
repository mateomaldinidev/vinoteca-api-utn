-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_03__order_items_snapshot.sql
-- Version : 1.0 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL aditivo sobre order_items
--
-- Requiere:
--   - V2_00__prevalidacion.sql sin bloqueos.
--
-- Efecto:
--   - product_name_snapshot       : nombre del producto al momento del pedido.
--   - variant_attribute_snapshot  : atributo de la variante al momento del pedido.
--
-- IMPORTANTE:
--   - Columnas NULL para no romper items historicos.
--   - NO se backfillean items historicos: products.name y
--     product_variants.attribute pueden haber cambiado desde la creacion.
--   - La app debe completar estos campos al crear pedidos nuevos.
--   - Script de ejecucion UNICA.
-- ============================================================================

-- Un ALTER por columna: cada AFTER apunta a una columna ya existente.
ALTER TABLE order_items
    ADD COLUMN product_name_snapshot VARCHAR(255) NULL
        COMMENT 'Snapshot del nombre del producto al crear el pedido.'
        AFTER product_variant_id;

ALTER TABLE order_items
    ADD COLUMN variant_attribute_snapshot VARCHAR(255) NULL
        COMMENT 'Snapshot del atributo de la variante al crear el pedido.'
        AFTER product_name_snapshot;

-- Verificacion rapida local (opcional, solo lectura)
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_items'
  AND COLUMN_NAME IN (
      'product_variant_id',
      'product_name_snapshot','variant_attribute_snapshot')
ORDER BY ORDINAL_POSITION;
