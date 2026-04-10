-- Order: add internal tender, refund, and reconciliation fields
ALTER TABLE product_order ADD COLUMN IF NOT EXISTS tender_type VARCHAR(30) NOT NULL DEFAULT 'INTERNAL_CREDIT';
ALTER TABLE product_order ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(12,2) DEFAULT 0;
ALTER TABLE product_order ADD COLUMN IF NOT EXISTS refund_reason VARCHAR(255);
ALTER TABLE product_order ADD COLUMN IF NOT EXISTS reconciled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product_order ADD COLUMN IF NOT EXISTS reconciled_at TIMESTAMP;
ALTER TABLE product_order ADD COLUMN IF NOT EXISTS reconciliation_ref VARCHAR(100);

-- Index for reconciliation queries
CREATE INDEX IF NOT EXISTS idx_order_reconciled ON product_order(reconciled) WHERE reconciled = FALSE;
