-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_06A__opt_orders_idempotency_pair.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL OPCIONAL e INDEPENDIENTE
--
-- ESTADO: OPCIONAL. No se aplica por defecto ni se considera aprobado por el
--         hecho de existir. Requiere aprobacion explicita e individual.
--
-- Requiere:
--   - V2_01 aplicado (columnas idempotency_key y checkout_request_hash).
--   - V2_00 sin bloqueos.
--
-- Efecto:
--   - CHECK chk_orders_idempotency_pair:
--     idempotency_key y checkout_request_hash deben estar ambos presentes
--     o ambos NULL. No valida el contenido del hash.
--
-- Rollback:
--   - V2_90__rollback.sql, bloque "06A" (descomentar solo si se aplico).
-- ============================================================================

-- Prevalidacion (solo lectura): debe devolver 0 antes de aplicar el CHECK.
SELECT COUNT(*) AS violations
FROM orders
WHERE (idempotency_key IS NULL) <> (checkout_request_hash IS NULL);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_idempotency_pair
    CHECK (
        (idempotency_key IS NULL AND checkout_request_hash IS NULL)
        OR
        (idempotency_key IS NOT NULL AND checkout_request_hash IS NOT NULL)
    );

-- Verificacion rapida local (solo lectura)
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_orders_idempotency_pair';
