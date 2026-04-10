-- Evidence encryption key management: add rotation tracking and retention policy
ALTER TABLE appeal_evidence ADD COLUMN IF NOT EXISTS encryption_key_version SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE appeal_evidence ADD COLUMN IF NOT EXISTS retention_expires_at TIMESTAMP;

-- Set default retention: 2 years from upload
UPDATE appeal_evidence SET retention_expires_at = uploaded_at + INTERVAL '2 years'
    WHERE retention_expires_at IS NULL;

-- Index for retention purge job
CREATE INDEX IF NOT EXISTS idx_evidence_retention ON appeal_evidence(retention_expires_at)
    WHERE retention_expires_at IS NOT NULL;
