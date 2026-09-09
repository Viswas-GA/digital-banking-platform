-- Placeholder migration; user registration tables come in Step 2
CREATE TABLE IF NOT EXISTS schema_version_marker (
    id          SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO schema_version_marker DEFAULT VALUES
ON CONFLICT (id) DO NOTHING;
