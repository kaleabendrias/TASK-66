-- Warehouses
CREATE TABLE warehouse (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    location    VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Inventory per product per warehouse
CREATE TABLE inventory_item (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT       NOT NULL REFERENCES product(id),
    warehouse_id        BIGINT       NOT NULL REFERENCES warehouse(id),
    quantity_on_hand    INTEGER      NOT NULL DEFAULT 0,
    quantity_reserved   INTEGER      NOT NULL DEFAULT 0,
    quantity_available  INTEGER      NOT NULL GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    low_stock_threshold INTEGER     NOT NULL DEFAULT 5,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, warehouse_id)
);

CREATE INDEX idx_inventory_low_stock ON inventory_item((quantity_on_hand - quantity_reserved))
    WHERE (quantity_on_hand - quantity_reserved) < 5;

-- Immutable inventory movement log
CREATE TABLE inventory_movement (
    id                  BIGSERIAL PRIMARY KEY,
    inventory_item_id   BIGINT       NOT NULL REFERENCES inventory_item(id),
    warehouse_id        BIGINT       NOT NULL REFERENCES warehouse(id),
    movement_type       VARCHAR(30)  NOT NULL,
    quantity            INTEGER      NOT NULL,
    balance_after       INTEGER      NOT NULL,
    reference_document  VARCHAR(255),
    operator_id         BIGINT       NOT NULL REFERENCES app_user(id),
    notes               TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_movement_item ON inventory_movement(inventory_item_id, created_at DESC);
CREATE INDEX idx_movement_operator ON inventory_movement(operator_id);
CREATE INDEX idx_movement_warehouse ON inventory_movement(warehouse_id);

-- Stock reservations with TTL
CREATE TABLE stock_reservation (
    id                  BIGSERIAL PRIMARY KEY,
    inventory_item_id   BIGINT       NOT NULL REFERENCES inventory_item(id),
    user_id             BIGINT       NOT NULL REFERENCES app_user(id),
    quantity            INTEGER      NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'HELD',
    idempotency_key     VARCHAR(100) UNIQUE,
    expires_at          TIMESTAMP    NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    confirmed_at        TIMESTAMP,
    cancelled_at        TIMESTAMP
);
CREATE INDEX idx_reservation_expires ON stock_reservation(expires_at) WHERE status = 'HELD';
CREATE INDEX idx_reservation_user ON stock_reservation(user_id);

-- Seed warehouses
INSERT INTO warehouse (name, code, location) VALUES
  ('Main Warehouse', 'WH-MAIN', 'Building A, Floor 1'),
  ('Overflow Storage', 'WH-OVER', 'Building B, Floor 2'),
  ('Returns Center', 'WH-RET', 'Building C, Floor 1');

-- Seed inventory
INSERT INTO inventory_item (product_id, warehouse_id, quantity_on_hand, quantity_reserved, low_stock_threshold) VALUES
  (1, 1, 120, 5, 10),
  (1, 2, 30, 0, 5),
  (2, 1, 300, 10, 20),
  (3, 1, 80, 0, 5),
  (4, 1, 400, 15, 25),
  (4, 2, 100, 0, 10),
  (5, 1, 25, 0, 5),
  (6, 1, 150, 8, 15),
  (6, 2, 50, 0, 5);
