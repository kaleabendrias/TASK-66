-- Replace loose reference fields with strict FK enforcement
-- For ORDER references: add actual FK to product_order
ALTER TABLE benefit_issuance_ledger ADD COLUMN IF NOT EXISTS order_id BIGINT REFERENCES product_order(id);
ALTER TABLE benefit_issuance_ledger ADD COLUMN IF NOT EXISTS incident_id BIGINT REFERENCES incident(id);

ALTER TABLE benefit_redemption_ledger ADD COLUMN IF NOT EXISTS order_id BIGINT REFERENCES product_order(id);
ALTER TABLE benefit_redemption_ledger ADD COLUMN IF NOT EXISTS incident_id BIGINT REFERENCES incident(id);

-- Constraint: at least one FK must be set when reference_type is specified
ALTER TABLE benefit_issuance_ledger ADD CONSTRAINT chk_issuance_ref
    CHECK (reference_type IS NULL OR order_id IS NOT NULL OR incident_id IS NOT NULL);

ALTER TABLE benefit_redemption_ledger ADD CONSTRAINT chk_redemption_ref
    CHECK (reference_type IS NULL OR order_id IS NOT NULL OR incident_id IS NOT NULL);

-- Indexes for FK lookups
CREATE INDEX IF NOT EXISTS idx_issuance_order ON benefit_issuance_ledger(order_id) WHERE order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_issuance_incident ON benefit_issuance_ledger(incident_id) WHERE incident_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_redemption_order ON benefit_redemption_ledger(order_id) WHERE order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_redemption_incident ON benefit_redemption_ledger(incident_id) WHERE incident_id IS NOT NULL;
