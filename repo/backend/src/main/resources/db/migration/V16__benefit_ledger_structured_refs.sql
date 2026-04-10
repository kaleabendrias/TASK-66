-- Replace free-form VARCHAR reference with structured FK relationships
-- Polymorphic: reference_type + reference_id pattern

ALTER TABLE benefit_issuance_ledger ADD COLUMN IF NOT EXISTS reference_type VARCHAR(30);
ALTER TABLE benefit_issuance_ledger ADD COLUMN IF NOT EXISTS reference_id BIGINT;

ALTER TABLE benefit_redemption_ledger ADD COLUMN IF NOT EXISTS reference_type VARCHAR(30);
ALTER TABLE benefit_redemption_ledger ADD COLUMN IF NOT EXISTS reference_id BIGINT;

-- Backfill existing records: parse "order:123" style references
UPDATE benefit_issuance_ledger SET reference_type = 'ORDER', reference_id = NULL WHERE reference IS NOT NULL AND reference_type IS NULL;
UPDATE benefit_redemption_ledger SET reference_type = 'ORDER', reference_id = NULL WHERE reference IS NOT NULL AND reference_type IS NULL;

-- Indexes for lookup
CREATE INDEX IF NOT EXISTS idx_issuance_ref ON benefit_issuance_ledger(reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_redemption_ref ON benefit_redemption_ledger(reference_type, reference_id);
