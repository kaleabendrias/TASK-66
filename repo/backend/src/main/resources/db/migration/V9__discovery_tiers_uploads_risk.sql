-- Discovery: add neighborhood, latitude, longitude, available_from, available_to to listing
ALTER TABLE listing ADD COLUMN IF NOT EXISTS neighborhood VARCHAR(100);
ALTER TABLE listing ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE listing ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE listing ADD COLUMN IF NOT EXISTS available_from DATE;
ALTER TABLE listing ADD COLUMN IF NOT EXISTS available_to DATE;

CREATE INDEX IF NOT EXISTS idx_listing_neighborhood ON listing(neighborhood);
CREATE INDEX IF NOT EXISTS idx_listing_availability ON listing(available_from, available_to);

-- Tier: transition from points to spend bands
ALTER TABLE member_tier RENAME COLUMN min_points TO min_spend;
ALTER TABLE member_tier RENAME COLUMN max_points TO max_spend;

-- Update tier values to spend bands (in currency units)
UPDATE member_tier SET min_spend = 0,     max_spend = 499   WHERE name = 'Bronze';
UPDATE member_tier SET min_spend = 500,   max_spend = 1999  WHERE name = 'Silver';
UPDATE member_tier SET min_spend = 2000,  max_spend = 9999  WHERE name = 'Gold';
UPDATE member_tier SET min_spend = 10000, max_spend = NULL  WHERE name = 'Platinum';

-- Member profile: rename points to total_spend
ALTER TABLE member_profile RENAME COLUMN points TO total_spend;

-- Points ledger: rename to spend_ledger
ALTER TABLE points_ledger RENAME TO spend_ledger;
ALTER TABLE spend_ledger RENAME COLUMN balance_after TO spend_after;

-- Benefit: add scope and mutual exclusion
ALTER TABLE benefit_item ADD COLUMN IF NOT EXISTS scope VARCHAR(50) NOT NULL DEFAULT 'ORDER';
ALTER TABLE benefit_item ADD COLUMN IF NOT EXISTS exclusion_group VARCHAR(50);

-- Set exclusion groups: DISCOUNT benefits are mutually exclusive
UPDATE benefit_item SET exclusion_group = 'DISCOUNT_GROUP' WHERE benefit_type = 'DISCOUNT';
UPDATE benefit_item SET scope = 'ORDER' WHERE benefit_type IN ('DISCOUNT', 'FREE_SHIPPING');
UPDATE benefit_item SET scope = 'ACCOUNT' WHERE benefit_type IN ('PRIORITY_SUPPORT', 'EXCLUSIVE_ACCESS');

-- Appeal evidence: file upload metadata table
CREATE TABLE IF NOT EXISTS appeal_evidence (
    id              BIGSERIAL PRIMARY KEY,
    appeal_id       BIGINT       NOT NULL REFERENCES appeal(id),
    original_name   VARCHAR(255) NOT NULL,
    stored_path     VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size       BIGINT       NOT NULL,
    uploaded_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_evidence_appeal ON appeal_evidence(appeal_id);

-- Risk analytics: add seller and ticket metrics tables
ALTER TABLE risk_score ADD COLUMN IF NOT EXISTS seller_complaint_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE risk_score ADD COLUMN IF NOT EXISTS open_incident_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE risk_score ADD COLUMN IF NOT EXISTS appeal_rejection_count INTEGER NOT NULL DEFAULT 0;

-- Update seed listing data with geo fields
UPDATE listing SET neighborhood = 'Downtown',     latitude = 40.7128, longitude = -74.0060, available_from = '2026-01-01', available_to = '2026-12-31' WHERE id = 1;
UPDATE listing SET neighborhood = 'Midtown',      latitude = 40.7549, longitude = -73.9840, available_from = '2026-01-01', available_to = '2026-12-31' WHERE id = 2;
UPDATE listing SET neighborhood = 'Upper West',   latitude = 40.7870, longitude = -73.9754, available_from = '2026-01-01', available_to = '2026-12-31' WHERE id = 3;
UPDATE listing SET neighborhood = 'East Village', latitude = 40.7265, longitude = -73.9815, available_from = '2026-02-01', available_to = '2026-11-30' WHERE id = 4;
UPDATE listing SET neighborhood = 'SoHo',         latitude = 40.7233, longitude = -73.9985, available_from = '2026-03-01', available_to = '2026-09-30' WHERE id = 5;
UPDATE listing SET neighborhood = 'Chelsea',      latitude = 40.7465, longitude = -74.0014, available_from = '2026-01-01', available_to = '2026-12-31' WHERE id = 6;
