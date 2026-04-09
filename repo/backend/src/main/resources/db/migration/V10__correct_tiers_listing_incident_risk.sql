-- Correct tier spend bands to strict 0-499, 500-1499, 1500+
UPDATE member_tier SET min_spend = 0,    max_spend = 499  WHERE name = 'Bronze';
UPDATE member_tier SET min_spend = 500,  max_spend = 1499 WHERE name = 'Silver';
UPDATE member_tier SET min_spend = 1500, max_spend = NULL WHERE name = 'Gold';

-- Reassign Platinum members to Gold BEFORE deleting the tier
UPDATE member_profile SET tier_id = (SELECT id FROM member_tier WHERE name = 'Gold')
  WHERE tier_id = (SELECT id FROM member_tier WHERE name = 'Platinum');

-- Remove orphaned ledger/benefit rows referencing Platinum packages BEFORE deleting
DELETE FROM benefit_redemption_ledger WHERE benefit_item_id IN (
  SELECT bi.id FROM benefit_item bi
  JOIN benefit_package bp ON bi.package_id = bp.id
  WHERE bp.tier_id = (SELECT id FROM member_tier WHERE name = 'Platinum')
);
DELETE FROM benefit_issuance_ledger WHERE benefit_item_id IN (
  SELECT bi.id FROM benefit_item bi
  JOIN benefit_package bp ON bi.package_id = bp.id
  WHERE bp.tier_id = (SELECT id FROM member_tier WHERE name = 'Platinum')
);
DELETE FROM benefit_item WHERE package_id IN (
  SELECT id FROM benefit_package WHERE tier_id = (SELECT id FROM member_tier WHERE name = 'Platinum')
);
DELETE FROM benefit_package WHERE tier_id = (SELECT id FROM member_tier WHERE name = 'Platinum');

-- Now safe to delete the Platinum tier
DELETE FROM member_tier WHERE name = 'Platinum';

-- Listing: add price, sqft, and layout columns for real discovery dimensions
ALTER TABLE listing ADD COLUMN IF NOT EXISTS price NUMERIC(12,2);
ALTER TABLE listing ADD COLUMN IF NOT EXISTS sqft INTEGER;
ALTER TABLE listing ADD COLUMN IF NOT EXISTS layout VARCHAR(50);

-- Backfill listing prices from product table
UPDATE listing SET price = p.price FROM product p WHERE listing.product_id = p.id AND listing.price IS NULL;

-- Incident: add structured address fields
ALTER TABLE incident ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE incident ADD COLUMN IF NOT EXISTS cross_street VARCHAR(255);

-- Risk: ensure 30-day window columns exist
-- (no schema change needed; logic change is in service layer)
