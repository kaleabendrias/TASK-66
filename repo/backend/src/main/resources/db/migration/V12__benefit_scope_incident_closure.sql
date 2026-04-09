-- Benefit item: add category/seller scoping and date windows
ALTER TABLE benefit_item ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES category(id);
ALTER TABLE benefit_item ADD COLUMN IF NOT EXISTS seller_id BIGINT REFERENCES app_user(id);
ALTER TABLE benefit_item ADD COLUMN IF NOT EXISTS valid_from TIMESTAMP;
ALTER TABLE benefit_item ADD COLUMN IF NOT EXISTS valid_to TIMESTAMP;

-- Incident: add closure code for RESOLVED transitions
ALTER TABLE incident ADD COLUMN IF NOT EXISTS closure_code VARCHAR(50);

-- Listing: add weekly_views for 7-day trending
ALTER TABLE listing ADD COLUMN IF NOT EXISTS weekly_views BIGINT NOT NULL DEFAULT 0;
UPDATE listing SET weekly_views = view_count / 4;  -- seed approximate weekly

-- Backfill some benefit scoping data
UPDATE benefit_item SET valid_from = '2026-01-01', valid_to = '2026-12-31' WHERE id <= 3;
UPDATE benefit_item SET category_id = 1 WHERE benefit_type = 'DISCOUNT' AND package_id = 1;
