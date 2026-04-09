-- Member tiers
CREATE TABLE member_tier (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    rank        INTEGER      NOT NULL UNIQUE,
    min_points  INTEGER      NOT NULL DEFAULT 0,
    max_points  INTEGER,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Benefit packages per tier
CREATE TABLE benefit_package (
    id          BIGSERIAL PRIMARY KEY,
    tier_id     BIGINT       NOT NULL REFERENCES member_tier(id),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE benefit_item (
    id          BIGSERIAL PRIMARY KEY,
    package_id  BIGINT       NOT NULL REFERENCES benefit_package(id),
    benefit_type VARCHAR(50) NOT NULL,
    benefit_value VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Member profile with encrypted phone
CREATE TABLE member_profile (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL UNIQUE REFERENCES app_user(id),
    tier_id         BIGINT       NOT NULL REFERENCES member_tier(id),
    points          INTEGER      NOT NULL DEFAULT 0,
    phone_encrypted VARCHAR(512),
    phone_masked    VARCHAR(20),
    joined_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Immutable points ledger
CREATE TABLE points_ledger (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT       NOT NULL REFERENCES member_profile(id),
    amount      INTEGER      NOT NULL,
    balance_after INTEGER    NOT NULL,
    entry_type  VARCHAR(30)  NOT NULL,
    reference   VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_points_ledger_member ON points_ledger(member_id);

-- Immutable issuance ledger
CREATE TABLE benefit_issuance_ledger (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT       NOT NULL REFERENCES member_profile(id),
    benefit_item_id BIGINT       NOT NULL REFERENCES benefit_item(id),
    issued_by       BIGINT       NOT NULL REFERENCES app_user(id),
    reference       VARCHAR(255),
    issued_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_issuance_member ON benefit_issuance_ledger(member_id);

-- Immutable redemption ledger
CREATE TABLE benefit_redemption_ledger (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT       NOT NULL REFERENCES member_profile(id),
    benefit_item_id BIGINT       NOT NULL REFERENCES benefit_item(id),
    reference       VARCHAR(255),
    redeemed_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_redemption_member ON benefit_redemption_ledger(member_id);

-- Seed tiers
INSERT INTO member_tier (name, rank, min_points, max_points) VALUES
  ('Bronze', 1, 0, 999),
  ('Silver', 2, 1000, 4999),
  ('Gold',   3, 5000, 19999),
  ('Platinum', 4, 20000, NULL);

-- Seed benefit packages
INSERT INTO benefit_package (tier_id, name, description) VALUES
  (1, 'Bronze Basics', 'Entry-level benefits'),
  (2, 'Silver Perks', 'Mid-tier benefits with discounts'),
  (3, 'Gold Rewards', 'Premium benefits including priority support'),
  (4, 'Platinum Elite', 'Top-tier exclusive benefits');

INSERT INTO benefit_item (package_id, benefit_type, benefit_value) VALUES
  (1, 'DISCOUNT', '5'),
  (2, 'DISCOUNT', '10'),
  (2, 'FREE_SHIPPING', 'true'),
  (3, 'DISCOUNT', '15'),
  (3, 'FREE_SHIPPING', 'true'),
  (3, 'PRIORITY_SUPPORT', 'true'),
  (4, 'DISCOUNT', '25'),
  (4, 'FREE_SHIPPING', 'true'),
  (4, 'PRIORITY_SUPPORT', 'true'),
  (4, 'EXCLUSIVE_ACCESS', 'true');

-- Seed member profiles for existing users
INSERT INTO member_profile (user_id, tier_id, points) VALUES
  (2, 1, 250),
  (3, 2, 1500),
  (4, 1, 100),
  (5, 1, 50),
  (6, 4, 25000);
