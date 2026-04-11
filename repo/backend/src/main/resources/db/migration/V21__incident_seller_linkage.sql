-- Explicit seller linkage on incidents.
--
-- Risk Analytics scores sellers based on incidents filed against *their*
-- listings. Previously it had to infer seller exposure from
-- reporter_id/assignee_id — neither of which identifies the seller being
-- complained about. This column makes the link explicit so the 30-day
-- seller-scoped window is meaningful.

ALTER TABLE incident ADD COLUMN IF NOT EXISTS seller_id BIGINT REFERENCES app_user(id);
CREATE INDEX IF NOT EXISTS idx_incident_seller ON incident(seller_id);
CREATE INDEX IF NOT EXISTS idx_incident_seller_created_at ON incident(seller_id, created_at);
