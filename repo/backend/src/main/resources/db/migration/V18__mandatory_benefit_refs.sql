-- Make reference_type mandatory on benefit ledgers
-- First backfill any NULL reference_types
UPDATE benefit_issuance_ledger SET reference_type = 'ORDER' WHERE reference_type IS NULL;
UPDATE benefit_redemption_ledger SET reference_type = 'ORDER' WHERE reference_type IS NULL;

-- Add NOT NULL constraints
ALTER TABLE benefit_issuance_ledger ALTER COLUMN reference_type SET NOT NULL;
ALTER TABLE benefit_redemption_ledger ALTER COLUMN reference_type SET NOT NULL;

-- Drop the old permissive CHECK and add strict one
ALTER TABLE benefit_issuance_ledger DROP CONSTRAINT IF EXISTS chk_issuance_ref;
ALTER TABLE benefit_issuance_ledger ADD CONSTRAINT chk_issuance_ref_strict
    CHECK (
        (reference_type = 'ORDER' AND order_id IS NOT NULL) OR
        (reference_type = 'INCIDENT' AND incident_id IS NOT NULL)
    );

ALTER TABLE benefit_redemption_ledger DROP CONSTRAINT IF EXISTS chk_redemption_ref;
ALTER TABLE benefit_redemption_ledger ADD CONSTRAINT chk_redemption_ref_strict
    CHECK (
        (reference_type = 'ORDER' AND order_id IS NOT NULL) OR
        (reference_type = 'INCIDENT' AND incident_id IS NOT NULL)
    );
