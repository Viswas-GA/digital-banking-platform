CREATE TABLE beneficiaries (
    id                   UUID PRIMARY KEY,
    user_id              UUID NOT NULL,
    nickname             VARCHAR(100) NOT NULL,
    account_number       VARCHAR(20) NOT NULL,
    account_holder_name  VARCHAR(200) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, account_number)
);

CREATE TABLE transactions (
    id                   UUID PRIMARY KEY,
    user_id              UUID NOT NULL,
    from_account_id      UUID NOT NULL,
    to_account_id        UUID NOT NULL,
    from_account_number  VARCHAR(20) NOT NULL,
    to_account_number    VARCHAR(20) NOT NULL,
    amount               NUMERIC(19, 2) NOT NULL,
    currency             VARCHAR(3) NOT NULL,
    type                 VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL,
    description          VARCHAR(255),
    reference            VARCHAR(50) NOT NULL UNIQUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at DESC);
