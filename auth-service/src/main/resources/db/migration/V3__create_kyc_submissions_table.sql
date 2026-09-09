CREATE TABLE kyc_submissions (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users (id),
    document_type    VARCHAR(50) NOT NULL,
    document_number  VARCHAR(100) NOT NULL,
    date_of_birth    DATE NOT NULL,
    address_line1    VARCHAR(255) NOT NULL,
    address_line2    VARCHAR(255),
    city             VARCHAR(100) NOT NULL,
    state            VARCHAR(100),
    postal_code      VARCHAR(20) NOT NULL,
    country          VARCHAR(100) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    rejection_reason VARCHAR(500),
    reviewed_at      TIMESTAMPTZ,
    reviewed_by      UUID REFERENCES users (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kyc_submissions_user_id ON kyc_submissions (user_id);
CREATE INDEX idx_kyc_submissions_status ON kyc_submissions (status);
