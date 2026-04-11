-- At-rest encryption for user PII (email and display_name).
--
-- The EncryptedStringConverter wraps values with AES-256/GCM plus a random IV,
-- producing non-deterministic, Base64-encoded ciphertexts. Two consequences:
--   1. Columns must hold the longer ciphertext (not the original plaintext length).
--   2. A unique constraint on the encrypted column is meaningless because the
--      same plaintext encrypts to different ciphertexts each write.
--
-- To preserve "each email can register once" we keep a separate deterministic
-- SHA-256 lookup hash. The plaintext never touches the database; only the
-- encrypted blob and the hash are persisted. Existing plaintext rows (e.g. from
-- V2 seed data) are migrated in-place at application start by
-- UserPiiEncryptionMigrationRunner.

ALTER TABLE app_user ALTER COLUMN email TYPE VARCHAR(512);
ALTER TABLE app_user ALTER COLUMN display_name TYPE VARCHAR(512);

ALTER TABLE app_user DROP CONSTRAINT IF EXISTS app_user_email_key;

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_lookup_hash VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_user_email_lookup_hash
    ON app_user(email_lookup_hash);
