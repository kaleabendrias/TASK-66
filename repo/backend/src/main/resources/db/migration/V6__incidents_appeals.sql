-- Incidents with SLA tracking
CREATE TABLE incident (
    id                  BIGSERIAL PRIMARY KEY,
    reporter_id         BIGINT       NOT NULL REFERENCES app_user(id),
    assignee_id         BIGINT       REFERENCES app_user(id),
    incident_type       VARCHAR(30)  NOT NULL,
    severity            VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    title               VARCHAR(300) NOT NULL,
    description         TEXT         NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    sla_ack_deadline    TIMESTAMP,
    sla_resolve_deadline TIMESTAMP,
    escalation_level    INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    acknowledged_at     TIMESTAMP,
    resolved_at         TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_incident_status ON incident(status);
CREATE INDEX idx_incident_assignee ON incident(assignee_id);
CREATE INDEX idx_incident_sla_ack ON incident(sla_ack_deadline) WHERE status = 'OPEN';
CREATE INDEX idx_incident_sla_resolve ON incident(sla_resolve_deadline) WHERE status IN ('OPEN', 'ACKNOWLEDGED');

CREATE TABLE incident_comment (
    id          BIGSERIAL PRIMARY KEY,
    incident_id BIGINT       NOT NULL REFERENCES incident(id),
    author_id   BIGINT       NOT NULL REFERENCES app_user(id),
    content     TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_comment_incident ON incident_comment(incident_id);

-- Escalation audit trail
CREATE TABLE incident_escalation_log (
    id          BIGSERIAL PRIMARY KEY,
    incident_id BIGINT       NOT NULL REFERENCES incident(id),
    from_level  INTEGER      NOT NULL,
    to_level    INTEGER      NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Appeals
CREATE TABLE appeal (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES app_user(id),
    related_entity_type VARCHAR(50)  NOT NULL,
    related_entity_id   BIGINT       NOT NULL,
    reason              TEXT         NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED',
    reviewer_id         BIGINT       REFERENCES app_user(id),
    review_notes        TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    reviewed_at         TIMESTAMP,
    resolved_at         TIMESTAMP
);
CREATE INDEX idx_appeal_status ON appeal(status);
CREATE INDEX idx_appeal_user ON appeal(user_id);

-- Seed sample incidents
INSERT INTO incident (reporter_id, incident_type, severity, title, description, status, sla_ack_deadline, sla_resolve_deadline) VALUES
  (2, 'ORDER_ISSUE', 'HIGH', 'Wrong item received', 'Received keyboard instead of headphones for order #1', 'OPEN', NOW() + INTERVAL '15 minutes', NOW() + INTERVAL '24 hours'),
  (2, 'PRODUCT_DEFECT', 'NORMAL', 'Scratched screen on USB-C Hub', 'Hub arrived with visible scratches on the casing', 'ACKNOWLEDGED', NOW() - INTERVAL '10 minutes', NOW() + INTERVAL '23 hours');

INSERT INTO appeal (user_id, related_entity_type, related_entity_id, reason, status) VALUES
  (3, 'PRODUCT', 5, 'Standing Desk listing was rejected but meets all criteria. Please reconsider.', 'SUBMITTED');
