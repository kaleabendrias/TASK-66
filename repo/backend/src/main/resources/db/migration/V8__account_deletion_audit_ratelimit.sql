-- Account deletion requests with cooling-off
CREATE TABLE account_deletion_request (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES app_user(id),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    requested_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    cooling_off_ends_at TIMESTAMP    NOT NULL,
    processed_at        TIMESTAMP,
    cancelled_at        TIMESTAMP
);
CREATE INDEX idx_deletion_status ON account_deletion_request(status);
CREATE INDEX idx_deletion_cooloff ON account_deletion_request(cooling_off_ends_at) WHERE status = 'PENDING';

-- Audit log with retention policy
CREATE TABLE audit_log (
    id                  BIGSERIAL PRIMARY KEY,
    entity_type         VARCHAR(50)  NOT NULL,
    entity_id           BIGINT       NOT NULL,
    action              VARCHAR(50)  NOT NULL,
    actor_id            BIGINT,
    old_value           JSONB,
    new_value           JSONB,
    ip_address          VARCHAR(45),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    retention_expires_at TIMESTAMP   NOT NULL
);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);
CREATE INDEX idx_audit_retention ON audit_log(retention_expires_at);

-- Login attempt tracking for rate limiting
CREATE TABLE login_attempt (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    ip_address  VARCHAR(45)  NOT NULL,
    success     BOOLEAN      NOT NULL,
    attempted_at TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_login_attempt_user ON login_attempt(username, attempted_at);

-- Risk analytics (local computation only)
CREATE TABLE risk_event (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user(id),
    event_type  VARCHAR(50)  NOT NULL,
    severity    VARCHAR(20)  NOT NULL DEFAULT 'LOW',
    details     JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_risk_event_user ON risk_event(user_id, created_at DESC);

CREATE TABLE risk_score (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES app_user(id),
    score       DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    factors     JSONB,
    computed_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_risk_score ON risk_score(score DESC);
