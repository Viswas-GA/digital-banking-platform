CREATE TABLE scheduled_transfers (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL,
    from_account_id    UUID NOT NULL,
    to_account_number  VARCHAR(20) NOT NULL,
    amount             NUMERIC(19, 2) NOT NULL,
    description        VARCHAR(255),
    scheduled_at       TIMESTAMPTZ NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason     VARCHAR(500),
    transaction_id     UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scheduled_transfers_user_id ON scheduled_transfers (user_id);
CREATE INDEX idx_scheduled_transfers_status_scheduled_at ON scheduled_transfers (status, scheduled_at);
