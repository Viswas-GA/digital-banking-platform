CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    account_number  VARCHAR(20) NOT NULL UNIQUE,
    account_type    VARCHAR(20) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    balance         NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_accounts_account_number ON accounts (account_number);
