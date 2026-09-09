CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY,
    event_id       UUID NOT NULL UNIQUE,
    actor_user_id  UUID NOT NULL,
    actor_email    VARCHAR(255) NOT NULL,
    actor_role     VARCHAR(20) NOT NULL,
    action         VARCHAR(50) NOT NULL,
    resource_type  VARCHAR(50) NOT NULL,
    resource_id    VARCHAR(100) NOT NULL,
    details        VARCHAR(2000),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs (action, created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id, created_at DESC);
