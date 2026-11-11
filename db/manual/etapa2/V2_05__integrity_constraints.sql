-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_05__integrity_constraints.sql
-- Version : 1.0 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL aditivo: CHECK constraints REQUERIDOS
--
-- Requiere:
--   - V2_00__prevalidacion.sql: las tres consultas de violaciones deben dar 0.
--
-- Efecto:
--   - product_variants.stock >= 0
--   - order_items.quantity  > 0
--   - stock_movements.quantity <> 0
--
-- Notas MySQL 8.0.46:
--   - CHECK se soporta y se aplica desde MySQL 8.0.16.
--   - Al agregar el CHECK, MySQL valida las filas existentes y falla si hay
--     violaciones. Ya se prevalido que no las hay.
--   - Los CHECK se aplican en INSERT y UPDATE.
--   - MySQL no permite CHECK que referencien otras tablas ni subconsultas.
--   - Un CHECK cuyo resultado es NULL se considera satisfecho; por eso se
--     usan columnas NOT NULL en estas tres validaciones.
--
-- IMPORTANTE:
--   - Script de ejecucion UNICA.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Stock no negativo
-- ----------------------------------------------------------------------------
ALTER TABLE product_variants
    ADD CONSTRAINT chk_product_variants_stock_non_negative
    CHECK (stock >= 0);

-- ----------------------------------------------------------------------------
-- 2. Cantidad de item de pedido positiva
--    Se usa > 0 (mas estricto que <> 0) porque un item de pedido no puede
--    tener cantidad negativa ni cero.
-- ----------------------------------------------------------------------------
ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_quantity_positive
    CHECK (quantity > 0);

-- ----------------------------------------------------------------------------
-- 3. Cantidad de movimiento de stock distinta de cero
--    Se permite negativa porque los ADJUSTMENT pueden descontar stock.
-- ----------------------------------------------------------------------------
ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_quantity_non_zero
    CHECK (quantity <> 0);

-- ----------------------------------------------------------------------------
-- 4. Verificacion rapida local (opcional, solo lectura)
-- ----------------------------------------------------------------------------
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME IN (
      'chk_product_variants_stock_non_negative',
      'chk_order_items_quantity_positive',
      'chk_stock_movements_quantity_non_zero')
ORDER BY CONSTRAINT_NAME;
