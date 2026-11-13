-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_06B__opt_orders_cancelled_coherence.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL OPCIONAL e INDEPENDIENTE
--
-- ESTADO: OPCIONAL. No se aplica por defecto ni se considera aprobado por el
--         hecho de existir. Requiere aprobacion explicita e individual.
--
-- Requiere:
--   - V2_01 aplicado (status, cancellation_reason, cancelled_at).
--   - V2_00 sin bloqueos.
--
-- Efecto:
--   - CHECK chk_orders_cancelled_at_status:
--     cancelled_at y cancellation_reason solo pueden existir si
--     status = 'CANCELLED'. Las filas historicas canceladas con NULL son
--     validas. COALESCE(status, '') evita que un status NULL pase el CHECK
--     por evaluacion a NULL.
--
-- Rollback:
--   - V2_90__rollback.sql, bloque "06B" (descomentar solo si se aplico).
-- ============================================================================

-- Prevalidacion (solo lectura): debe devolver 0 antes de aplicar el CHECK.
SELECT COUNT(*) AS violations
FROM orders
WHERE (cancelled_at IS NOT NULL AND COALESCE(status, '') <> 'CANCELLED')
   OR (cancellation_reason IS NOT NULL AND COALESCE(status, '') <> 'CANCELLED');

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_cancelled_at_status
    CHECK (
        (cancelled_at IS NULL OR COALESCE(status, '') = 'CANCELLED')
        AND (cancellation_reason IS NULL OR COALESCE(status, '') = 'CANCELLED')
    );

-- Verificacion rapida local (solo lectura)
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_orders_cancelled_at_status';
