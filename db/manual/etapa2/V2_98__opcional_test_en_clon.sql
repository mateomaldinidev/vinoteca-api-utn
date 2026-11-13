-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_98__opcional_test_en_clon.sql
-- Version : 1.3 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : PRUEBA OPCIONAL DE APLICACION DE CHECK Y FK
--
-- ############################################################################
-- ##  NO EJECUTAR ESTE ARCHIVO EN corralon_db NI EN NINGUNA BASE REAL.      ##
-- ##  Contiene UPDATE a proposito para demostrar que los CHECK y la FK      ##
-- ##  compuesta se aplican. Ejecutar SOLO en una copia descartable (clon).  ##
-- ##  Cada bloque termina en ROLLBACK, pero aun asi no debe usarse en real. ##
-- ############################################################################
--
-- GUARDIA OBLIGATORIA DE CLON
--   1. El USE redirige la sesion al clon. Si la base no existe, el cliente
--      aborta en modo batch y no se ejecuta ninguna prueba. Si el archivo se
--      lanza por error contra corralon_db, el USE cambia a corralon_db_checktest
--      antes de cualquier UPDATE: las pruebas NO tocan la base real.
--   2. La consulta siguiente confirma el nombre. Si clone_guard es 'STOP',
--      NO continuar.
--   3. Si el clon tiene otro nombre, cambiar el literal en USE y en la
--      consulta; nunca quitar la guardia.
USE corralon_db_checktest;

SELECT DATABASE() AS current_database,
       IF(DATABASE() = 'corralon_db_checktest', 'OK',
          'STOP: NO ES EL CLON') AS clone_guard;
--
-- PREPARACION DEL CLON (fuera de este archivo, con autorizacion):
--   IMPORTANTE: el dump del clon NO debe usar --databases, porque incluye
--   CREATE DATABASE/USE corralon_db y la restauracion terminaria escribiendo
--   en la base real. Generar el dump con el nombre de la base al final:
--   mysqldump ... --single-transaction --quick --hex-blob --no-tablespaces \
--     corralon_db > backup.sql
--   mysql ... -e "CREATE DATABASE corralon_db_checktest"
--   mysql ... corralon_db_checktest < backup.sql
--   mysql ... -D corralon_db_checktest < V2_01__orders_checkout.sql
--   mysql ... -D corralon_db_checktest < V2_02__orders_delivery_snapshot.sql
--   mysql ... -D corralon_db_checktest < V2_03__order_items_snapshot.sql
--   mysql ... -D corralon_db_checktest < V2_04__stock_movements_traceability.sql
--   mysql ... -D corralon_db_checktest < V2_05__integrity_constraints.sql
--   (opcional, solo los que se van a probar)
--   mysql ... -D corralon_db_checktest < V2_06A__opt_orders_idempotency_pair.sql
--   mysql ... -D corralon_db_checktest < V2_06B__opt_orders_cancelled_coherence.sql
--   mysql ... -D corralon_db_checktest < V2_06C__opt_orders_cancellation_reason.sql
--   mysql ... -D corralon_db_checktest < V2_06D__opt_stock_movements_resulting_stock.sql
--
-- REEMPLAZO DE IDENTIFICADORES
--   Antes de ejecutar, reemplazar cada <...> por un id existente en el clon:
--     <VARIANT_ID>      product_variants.id con stock >= 1
--     <ORDER_ITEM_ID>   order_items.id existente
--     <ORDER_ID>        order_items.order_id del item elegido
--     <OTHER_ORDER_ID>  orders.id existente distinto de <ORDER_ID>
--     <MOVEMENT_ID>     stock_movements.id existente (preferentemente con
--                       order_id y order_item_id NULL)
--     <NON_CANCELLED_ORDER_ID> orders.id con status <> 'CANCELLED'
--     <CANCELLED_ORDER_ID>     orders.id con status = 'CANCELLED'
--   Consultas para obtenerlos:
--     SELECT id, stock FROM product_variants ORDER BY id LIMIT 5;
--     SELECT oi.id AS order_item_id, oi.order_id, o2.id AS other_order_id
--     FROM order_items oi
--     JOIN orders o2 ON o2.id <> oi.order_id
--     ORDER BY oi.id LIMIT 5;
--     SELECT id, order_id, order_item_id FROM stock_movements ORDER BY id LIMIT 5;
--     SELECT id, status FROM orders WHERE status <> 'CANCELLED' ORDER BY id LIMIT 5;
--     SELECT id, status FROM orders WHERE status = 'CANCELLED' ORDER BY id LIMIT 5;
--
-- COMO EJECUTAR
--   - Usar un cliente interactivo (mysql CLI, Workbench, DBeaver).
--   - Pegar UN bloque por vez, de START TRANSACTION a ROLLBACK.
--   - Si el UPDATE produce el error esperado, MySQL NO cierra la transaccion
--     automaticamente: ejecutar ROLLBACK; a continuacion.
--   - En modo batch (`mysql < archivo`), el cliente se detiene en el primer
--     error. Por eso este archivo es una guia y NO debe correrse completo de
--     una sola vez.
--   - Nunca usar `--force` para ocultar errores inesperados.
--   - Los TEST 4 y 7 a 9 son opcionales: ejecutarlos solo si se aplico el
--     script indicado. En cada caso debe fallar la constraint nombrada.
--
-- COMO COMPROBAR QUE EL ERROR ES EL ESPERADO
--   - Error 3819 = CHECK constraint violated. El mensaje incluye el nombre
--     exacto de la constraint. Debe coincidir con el nombre del bloque.
--   - Error 1452 = Cannot add or update a child row: a foreign key constraint
--     fails. El mensaje incluye el nombre de la FK.
--   - Si el UPDATE NO da error, el CHECK/FK no se aplico: DETENERSE.
--
-- VALORES ORIGINALES (guardar antes de empezar)
--   SELECT id, stock FROM product_variants WHERE id = <VARIANT_ID>;
--   SELECT id, quantity FROM order_items WHERE id = <ORDER_ITEM_ID>;
--   SELECT id, quantity, order_id, order_item_id, resulting_stock
--     FROM stock_movements WHERE id = <MOVEMENT_ID>;
--   Repetir estas consultas despues de cada ROLLBACK y comparar.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- TEST 1: CHECK product_variants.stock >= 0
-- Error esperado: 3819, chk_product_variants_stock_non_negative
-- ----------------------------------------------------------------------------
START TRANSACTION;
UPDATE product_variants SET stock = -1 WHERE id = <VARIANT_ID>;
-- ERROR ESPERADO: 3819 - Check constraint 'chk_product_variants_stock_non_negative' is violated.
ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 2: CHECK order_items.quantity > 0
-- Error esperado: 3819, chk_order_items_quantity_positive
-- ----------------------------------------------------------------------------
START TRANSACTION;
UPDATE order_items SET quantity = 0 WHERE id = <ORDER_ITEM_ID>;
-- ERROR ESPERADO: 3819 - Check constraint 'chk_order_items_quantity_positive' is violated.
ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 3: CHECK stock_movements.quantity <> 0
-- Error esperado: 3819, chk_stock_movements_quantity_non_zero
-- ----------------------------------------------------------------------------
START TRANSACTION;
UPDATE stock_movements SET quantity = 0 WHERE id = <MOVEMENT_ID>;
-- ERROR ESPERADO: 3819 - Check constraint 'chk_stock_movements_quantity_non_zero' is violated.
ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 4 (solo si se aplico V2_06D): CHECK resulting_stock >= 0
-- Error esperado: 3819, chk_stock_movements_resulting_stock_non_negative
-- ----------------------------------------------------------------------------
-- START TRANSACTION;
-- UPDATE stock_movements SET resulting_stock = -1 WHERE id = <MOVEMENT_ID>;
-- -- ERROR ESPERADO: 3819 - chk_stock_movements_resulting_stock_non_negative
-- ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 5: CHECK chk_stock_movements_order_item_requires_order
-- Error esperado: 3819, chk_stock_movements_order_item_requires_order
-- ----------------------------------------------------------------------------
START TRANSACTION;
UPDATE stock_movements
SET order_id = NULL, order_item_id = <ORDER_ITEM_ID>
WHERE id = <MOVEMENT_ID>;
-- ERROR ESPERADO: 3819 - chk_stock_movements_order_item_requires_order
ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 6: FK compuesta fk_stock_movements_order_item_order
-- Primero se asigna un par valido (item + pedido al que pertenece); luego se
-- intenta cambiar solo el pedido por otro existente.
-- Error esperado: 1452, fk_stock_movements_order_item_order
-- ----------------------------------------------------------------------------
START TRANSACTION;
UPDATE stock_movements
SET order_id = <ORDER_ID>, order_item_id = <ORDER_ITEM_ID>
WHERE id = <MOVEMENT_ID>;
-- Verificar el par valido antes del intento invalido:
SELECT id, order_id, order_item_id
FROM stock_movements WHERE id = <MOVEMENT_ID>;

UPDATE stock_movements
SET order_id = <OTHER_ORDER_ID>
WHERE id = <MOVEMENT_ID>;
-- ERROR ESPERADO: 1452 - fk_stock_movements_order_item_order
ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 7 (solo si se aplico V2_06A): CHECK chk_orders_idempotency_pair
-- Error esperado: 3819, chk_orders_idempotency_pair
-- ----------------------------------------------------------------------------
-- START TRANSACTION;
-- UPDATE orders
-- SET idempotency_key = 'audit-check', checkout_request_hash = NULL
-- WHERE id = <ORDER_ID>;
-- -- ERROR ESPERADO: 3819 - chk_orders_idempotency_pair
-- ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 8 (solo si se aplico V2_06B): CHECK chk_orders_cancelled_at_status
-- El pedido elegido NO debe estar CANCELLED.
-- Error esperado: 3819, chk_orders_cancelled_at_status
-- ----------------------------------------------------------------------------
-- START TRANSACTION;
-- UPDATE orders
-- SET cancellation_reason = 'EXPIRED'
-- WHERE id = <NON_CANCELLED_ORDER_ID>;
-- -- ERROR ESPERADO: 3819 - chk_orders_cancelled_at_status
-- ROLLBACK;

-- ----------------------------------------------------------------------------
-- TEST 9 (solo si se aplico V2_06C): CHECK chk_orders_cancellation_reason
-- El pedido elegido debe estar CANCELLED (si tambien se aplico V2_06B).
-- Error esperado: 3819, chk_orders_cancellation_reason
-- ----------------------------------------------------------------------------
-- START TRANSACTION;
-- UPDATE orders
-- SET cancellation_reason = 'INVALID_REASON'
-- WHERE id = <CANCELLED_ORDER_ID>;
-- -- ERROR ESPERADO: 3819 - chk_orders_cancellation_reason
-- ROLLBACK;

-- ----------------------------------------------------------------------------
-- VERIFICACION FINAL: los ROLLBACK no dejaron cambios.
-- Comparar contra los valores guardados al inicio.
-- ----------------------------------------------------------------------------
SELECT id, stock FROM product_variants WHERE id = <VARIANT_ID>;
SELECT id, quantity FROM order_items WHERE id = <ORDER_ITEM_ID>;
SELECT id, quantity, order_id, order_item_id, resulting_stock
FROM stock_movements WHERE id = <MOVEMENT_ID>;
