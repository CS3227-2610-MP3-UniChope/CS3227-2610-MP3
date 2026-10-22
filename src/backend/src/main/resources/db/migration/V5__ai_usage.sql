-- Durable AI admission per SoC key slot (STUDENT, TUTOR, ADMIN). Metadata only: no prompt,
-- input or model output is stored. The key-state row is locked to serialize admission per key.
CREATE TABLE ai_key_state (
    key_slot text PRIMARY KEY CHECK (key_slot IN ('STUDENT', 'TUTOR', 'ADMIN'))
);
INSERT INTO ai_key_state (key_slot) VALUES ('STUDENT'), ('TUTOR'), ('ADMIN');

CREATE TABLE ai_attempts (
    id uuid PRIMARY KEY,
    key_slot text NOT NULL REFERENCES ai_key_state(key_slot),
    actor_id uuid NOT NULL,
    task text NOT NULL,
    admitted_at timestamptz NOT NULL,
    reserved_micros bigint NOT NULL CHECK (reserved_micros > 0),
    charged_micros bigint NOT NULL CHECK (charged_micros >= 0),
    in_flight_until timestamptz,
    completed_at timestamptz
);
CREATE INDEX ai_attempts_slot_time ON ai_attempts (key_slot, admitted_at);
CREATE INDEX ai_attempts_actor_time ON ai_attempts (actor_id, admitted_at);

-- Admin-selected chat model for all AI requests. No row means the default (qwen3.5:9b).
CREATE TABLE ai_settings (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    model text NOT NULL CHECK (btrim(model) <> ''),
    updated_at timestamptz NOT NULL,
    updated_by uuid
);
