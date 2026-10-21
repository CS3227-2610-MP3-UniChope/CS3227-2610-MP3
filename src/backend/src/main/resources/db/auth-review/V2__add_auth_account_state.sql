-- Explicit isolated review location only. Existing credentialless rows must fail.
ALTER TABLE users
    ADD COLUMN password_hash text NOT NULL CHECK (password_hash !~ '^[[:space:]]*$'),
    ADD COLUMN session_generation bigint NOT NULL DEFAULT 0 CHECK (session_generation >= 0);
