-- ============================================================================
-- Etapa 2 - Checkout, vencimiento y trazabilidad
-- Archivo : V2_04__stock_movements_traceability.sql
-- Version : 1.2 (propuesta, NO ejecutada)
-- Fecha   : 2026-11-10
-- Motor   : MySQL 8.0.46
-- Tipo    : DDL aditivo sobre order_items y stock_movements
--
-- Requiere:
--   - V2_00__prevalidacion.sql sin bloqueos.
--   - V2_01 aplicado (orders existe y tiene las columnas de checkout).
--   - Tabla order_items existente (V2_03 agrega snapshots; no es requisito
--     tecnico de este script, pero se ejecuta antes por orden logico).
--
-- Efecto:
--   - order_items: UNIQUE (id, order_id) como clave referenciada.
--   - stock_movements.order_id            : pedido asociado (NULL si no aplica).
--   - stock_movements.order_item_id       : item asociado (NULL si no aplica).
--   - stock_movements.resulting_stock     : stock resultante del movimiento.
--   - stock_movements.created_by_user_id  : usuario que ejecuto el movimiento
--                                           (NULL = sistema).
--   - CHECK que exige order_id cuando hay order_item_id.
--   - FK compuesta (order_item_id, order_id) -> order_items (id, order_id).
--   - FKs e indices para consultas y trazabilidad.
--
-- Decisiones aplicadas:
--   - Columnas NULL para no inventar relaciones historicas.
--   - resulting_stock NULL en filas historicas (no se reconstruye).
--   - created_by_user_id NULL en filas historicas y en expiraciones automaticas.
--   - Se agrega UNIQUE (id, order_id) en order_items como clave referenciada
--     por la FK compuesta. Es redundante con la PK, pero MySQL exige un
--     indice sobre las columnas referenciadas y un UNIQUE es el formato
--     estandar y compatible con versiones futuras (restrict_fk_on_non_standard_key).
--   - Se reemplaza la FK independiente order_item_id -> order_items(id) por
--     la FK compuesta. La compuesta ya garantiza que el item existe y que
--     pertenece al pedido indicado.
--   - Se conserva fk_stock_movements_order porque un movimiento puede
--     referenciar solo un pedido, sin item.
--   - ON DELETE RESTRICT para order/order_item (no perder trazabilidad).
--   - FK created_by con ON DELETE SET NULL.
--
-- IMPORTANTE:
--   - NO se relacionan movimientos historicos con pedidos por inferencia.
--   - Los 17 movimientos existentes quedan con order_id/order_item_id NULL.
--   - La app debe completar estos campos en los movimientos nuevos.
--   - Script de ejecucion UNICA.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. Precondiciones de la FK compuesta (solo lectura)
--    - order_items.order_id debe ser NOT NULL.
--    - Los tipos de las columnas referenciadas deben coincidir.
--    - La unicidad de (id, order_id) se garantiza con el UNIQUE del paso 1.
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
        (TABLE_NAME = 'order_items' AND COLUMN_NAME IN ('id', 'order_id'))
     OR (TABLE_NAME = 'orders' AND COLUMN_NAME = 'id')
      )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- ----------------------------------------------------------------------------
-- 1. Clave unica referenciada por la FK compuesta
--    (id) ya es PK; (id, order_id) es el UNIQUE adicional que MySQL usa como
--    destino de la referencia compuesta.
-- ----------------------------------------------------------------------------
ALTER TABLE order_items
    ADD UNIQUE KEY uk_order_items_id_order (id, order_id);

-- ----------------------------------------------------------------------------
-- 2. Columnas nuevas
-- ----------------------------------------------------------------------------
-- Un ALTER por columna: cada AFTER apunta a una columna ya existente.
ALTER TABLE stock_movements
    ADD COLUMN order_id BIGINT NULL
        COMMENT 'Pedido asociado. NULL en movimientos historicos o manuales.'
        AFTER variant_id;

ALTER TABLE stock_movements
    ADD COLUMN order_item_id BIGINT NULL
        COMMENT 'Item de pedido asociado. NULL en movimientos historicos o manuales.'
        AFTER order_id;

ALTER TABLE stock_movements
    ADD COLUMN resulting_stock INT NULL
        COMMENT 'Stock de la variante despues de aplicar el movimiento. NULL en historicos.'
        AFTER quantity;

ALTER TABLE stock_movements
    ADD COLUMN created_by_user_id BIGINT NULL
        COMMENT 'Usuario que ejecuto el movimiento. NULL = sistema o historico.'
        AFTER reason;

-- ----------------------------------------------------------------------------
-- 3. Indices
--    - idx_stock_movements_order_id: soporta fk_stock_movements_order y las
--      consultas por pedido.
--    - idx_stock_movements_order_item_order: columnas de la FK compuesta en
--      el mismo orden. Su prefijo (order_item_id) tambien sirve para buscar
--      por item, por lo que NO se crea un indice separado sobre order_item_id.
--    - idx_stock_movements_created_by_user_id: soporta la FK de usuario.
-- ----------------------------------------------------------------------------
ALTER TABLE stock_movements
    ADD KEY idx_stock_movements_order_id (order_id),
    ADD KEY idx_stock_movements_order_item_order (order_item_id, order_id),
    ADD KEY idx_stock_movements_created_by_user_id (created_by_user_id);

-- ----------------------------------------------------------------------------
-- 4. Coherencia previa a la FK compuesta
--    InnoDB usa MATCH SIMPLE: una FK compuesta NO se valida si alguna de sus
--    columnas es NULL. Este CHECK obligatorio evita el caso
--    order_item_id NOT NULL + order_id NULL, de modo que la FK compuesta se
--    aplique siempre que exista un item.
-- ----------------------------------------------------------------------------
ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_order_item_requires_order
    CHECK (order_item_id IS NULL OR order_id IS NOT NULL);

-- ----------------------------------------------------------------------------
-- 5. Foreign keys
--    - fk_stock_movements_order: movimiento asociado solo a un pedido.
--    - fk_stock_movements_order_item_order: el item debe existir y pertenecer
--      al pedido indicado.
--    - fk_stock_movements_created_by: usuario, SET NULL al borrar.
--    No se crea fk_stock_movements_order_item independiente: la FK compuesta
--    la reemplaza y evita redundancia.
-- ----------------------------------------------------------------------------
ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_stock_movements_order_item_order
        FOREIGN KEY (order_item_id, order_id) REFERENCES order_items (id, order_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_stock_movements_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE RESTRICT;

-- ----------------------------------------------------------------------------
-- 6. Verificacion rapida local (opcional, solo lectura)
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'stock_movements'
  AND COLUMN_NAME IN (
      'order_id','order_item_id','resulting_stock','created_by_user_id')
ORDER BY ORDINAL_POSITION;

SELECT CONSTRAINT_NAME, ORDINAL_POSITION, COLUMN_NAME,
       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'stock_movements'
  AND CONSTRAINT_NAME IN (
      'fk_stock_movements_order',
      'fk_stock_movements_order_item_order',
      'fk_stock_movements_created_by')
ORDER BY CONSTRAINT_NAME, ORDINAL_POSITION;

SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'stock_movements'
  AND INDEX_NAME IN (
      'idx_stock_movements_order_id',
      'idx_stock_movements_order_item_order',
      'idx_stock_movements_created_by_user_id')
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_items'
  AND INDEX_NAME = 'uk_order_items_id_order'
ORDER BY SEQ_IN_INDEX;
