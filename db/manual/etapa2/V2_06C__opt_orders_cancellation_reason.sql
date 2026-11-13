-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_06C__opt_orders_cancellation_reason.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL OPCIONAL e INDEPENDIENTE
--
-- ESTADO: OPCIONAL. La lista de motivos esta ACORDADA (D3), pero la
--         aplicacion de este CHECK sigue siendo opcional y requiere
--         aprobacion explicita e individual.
--
-- Requiere:
--   - V2_01 aplicado (cancellation_reason).
--   - V2_00 sin bloqueos.
--
-- Efecto:
--   - CHECK chk_orders_cancellation_reason:
--     cancellation_reason NULL o uno de los motivos acordados:
--       EXPIRED, USER_CANCELLED, ADMIN_CANCELLED, PAYMENT_REJECTED, OTHER.
--
-- IMPORTANTE:
--   - Si el negocio define motivos adicionales, editar la lista ANTES de
--     aplicar. Una vez aplicado, un motivo fuera de la lista sera rechazado.
--   - Si se prefiere no acoplar la base a los motivos, no aplicar este script.
--
-- Rollback:
--   - V2_90__rollback.sql, bloque "06C" (descomentar solo si se aplico).
-- ============================================================================

-- Prevalidacion (solo lectura): no debe devolver motivos fuera de la lista.
SELECT cancellation_reason, COUNT(*) AS violations
FROM orders
WHERE cancellation_reason IS NOT NULL
  AND cancellation_reason NOT IN (
      'EXPIRED',
      'USER_CANCELLED',
      'ADMIN_CANCELLED',
      'PAYMENT_REJECTED',
      'OTHER'
  )
GROUP BY cancellation_reason;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_cancellation_reason
    CHECK (
        cancellation_reason IS NULL
        OR cancellation_reason IN (
            'EXPIRED',
            'USER_CANCELLED',
            'ADMIN_CANCELLED',
            'PAYMENT_REJECTED',
            'OTHER'
        )
    );

-- Verificacion rapida local (solo lectura)
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_orders_cancellation_reason';
