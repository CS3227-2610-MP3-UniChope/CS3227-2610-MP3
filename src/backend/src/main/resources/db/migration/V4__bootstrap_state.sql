CREATE TABLE bootstrap_state (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    completed_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
