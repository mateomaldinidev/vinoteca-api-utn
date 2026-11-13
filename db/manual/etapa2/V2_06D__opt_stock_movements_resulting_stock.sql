-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_06D__opt_stock_movements_resulting_stock.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL OPCIONAL e INDEPENDIENTE
--
-- ESTADO: OPCIONAL. No se aplica por defecto ni se considera aprobado por el
--         hecho de existir. Requiere aprobacion explicita e individual.
--
-- Requiere:
--   - V2_04 aplicado (columna resulting_stock).
--   - V2_00 sin bloqueos.
--
-- Efecto:
--   - CHECK chk_stock_movements_resulting_stock_non_negative:
--     resulting_stock NULL (historicos) o >= 0.
--
-- Rollback:
--   - V2_90__rollback.sql, bloque "06D" (descomentar solo si se aplico).
-- ============================================================================

-- Prevalidacion (solo lectura): debe devolver 0 antes de aplicar el CHECK.
SELECT COUNT(*) AS violations
FROM stock_movements
WHERE resulting_stock < 0;

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_resulting_stock_non_negative
    CHECK (resulting_stock IS NULL OR resulting_stock >= 0);

-- Verificacion rapida local (solo lectura)
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_stock_movements_resulting_stock_non_negative';
