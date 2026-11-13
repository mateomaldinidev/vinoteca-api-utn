-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_02__orders_delivery_snapshot.sql
-- Version : 1.0 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL aditivo sobre orders
--
-- Requiere:
--   - V2_00__prevalidacion.sql sin bloqueos.
--   - V2_01__orders_checkout.sql aplicado (no es dependencia tecnica, pero
--     mantiene el orden logico de la etapa).
--
-- Efecto:
--   - Snapshot completo de la direccion de entrega al momento del checkout.
--   - Se conserva orders.address_id como referencia original a addresses.id.
--   - Las columnas son NULL para PICKUP y para pedidos historicos.
--
-- IMPORTANTE:
--   - NO se backfillean pedidos historicos: no se puede garantizar que la
--     direccion actual sea la misma que la usada al crear el pedido.
--   - La app debe completar estos campos al crear pedidos nuevos.
--   - Script de ejecucion UNICA.
-- ============================================================================

-- Un ALTER por columna: cada AFTER apunta a una columna ya existente.
ALTER TABLE orders
    ADD COLUMN delivery_street VARCHAR(255) NULL
        COMMENT 'Snapshot: calle de entrega.'
        AFTER delivery_type;

ALTER TABLE orders
    ADD COLUMN delivery_street_number VARCHAR(255) NULL
        COMMENT 'Snapshot: numero de calle de entrega.'
        AFTER delivery_street;

ALTER TABLE orders
    ADD COLUMN delivery_floor VARCHAR(255) NULL
        COMMENT 'Snapshot: piso de entrega.'
        AFTER delivery_street_number;

ALTER TABLE orders
    ADD COLUMN delivery_apartment_number VARCHAR(255) NULL
        COMMENT 'Snapshot: departamento de entrega.'
        AFTER delivery_floor;

ALTER TABLE orders
    ADD COLUMN delivery_city VARCHAR(255) NULL
        COMMENT 'Snapshot: ciudad de entrega.'
        AFTER delivery_apartment_number;

ALTER TABLE orders
    ADD COLUMN delivery_zip_code VARCHAR(255) NULL
        COMMENT 'Snapshot: codigo postal de entrega.'
        AFTER delivery_city;

-- Verificacion rapida local (opcional, solo lectura)
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND COLUMN_NAME IN (
      'address_id',
      'delivery_street','delivery_street_number','delivery_floor',
      'delivery_apartment_number','delivery_city','delivery_zip_code')
ORDER BY ORDINAL_POSITION;
