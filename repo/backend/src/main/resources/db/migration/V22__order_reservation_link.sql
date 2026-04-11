-- Every order is now backed by a stock_reservation. This column is the
-- contract handle the cancel/fail compensation path uses to release the
-- hold (or roll back the on-hand if the order was already CONFIRMED).
ALTER TABLE product_order
    ADD COLUMN IF NOT EXISTS reservation_id BIGINT REFERENCES stock_reservation(id);

CREATE INDEX IF NOT EXISTS idx_product_order_reservation
    ON product_order(reservation_id);
