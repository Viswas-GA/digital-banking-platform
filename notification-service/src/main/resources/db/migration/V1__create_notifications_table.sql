CREATE TABLE notifications (
    id           UUID PRIMARY KEY,
    event_id     UUID NOT NULL UNIQUE,
    user_id      UUID NOT NULL,
    event_type   VARCHAR(50) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    message      VARCHAR(1000) NOT NULL,
    reference_id UUID,
    read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
