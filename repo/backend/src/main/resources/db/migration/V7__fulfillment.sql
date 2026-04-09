-- Fulfillment tracking
CREATE TABLE fulfillment (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES product_order(id),
    warehouse_id    BIGINT       NOT NULL REFERENCES warehouse(id),
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    operator_id     BIGINT       REFERENCES app_user(id),
    tracking_info   VARCHAR(255),
    idempotency_key VARCHAR(100) UNIQUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_fulfillment_order ON fulfillment(order_id);
CREATE INDEX idx_fulfillment_status ON fulfillment(status);

CREATE TABLE fulfillment_step (
    id              BIGSERIAL PRIMARY KEY,
    fulfillment_id  BIGINT       NOT NULL REFERENCES fulfillment(id),
    step_name       VARCHAR(50)  NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    operator_id     BIGINT       REFERENCES app_user(id),
    notes           TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP
);
CREATE INDEX idx_step_fulfillment ON fulfillment_step(fulfillment_id);

-- Seed fulfillments for existing orders
INSERT INTO fulfillment (order_id, warehouse_id, status, operator_id) VALUES
  (1, 1, 'PENDING', NULL),
  (2, 1, 'SHIPPED', 4),
  (3, 1, 'DELIVERED', 4);

INSERT INTO fulfillment_step (fulfillment_id, step_name, status, operator_id, completed_at) VALUES
  (2, 'PICK', 'COMPLETED', 4, NOW() - INTERVAL '2 days'),
  (2, 'PACK', 'COMPLETED', 4, NOW() - INTERVAL '2 days'),
  (2, 'SHIP', 'COMPLETED', 4, NOW() - INTERVAL '1 day'),
  (3, 'PICK', 'COMPLETED', 4, NOW() - INTERVAL '5 days'),
  (3, 'PACK', 'COMPLETED', 4, NOW() - INTERVAL '5 days'),
  (3, 'SHIP', 'COMPLETED', 4, NOW() - INTERVAL '4 days'),
  (3, 'DELIVER', 'COMPLETED', 4, NOW() - INTERVAL '2 days');
