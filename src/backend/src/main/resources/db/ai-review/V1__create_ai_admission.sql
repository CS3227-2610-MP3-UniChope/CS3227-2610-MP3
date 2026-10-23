-- REVIEW-ONLY schema for isolated AI admission tests; not an application migration.
-- Production migration activation requires cross-role review and a separate decision.
CREATE TABLE ai_quota_state (
    id smallint PRIMARY KEY CHECK (id = 1)
);
INSERT INTO ai_quota_state(id) VALUES (1);

CREATE TABLE ai_attempt (
    request_id uuid PRIMARY KEY,
    actor_id uuid NOT NULL,
    task text NOT NULL CHECK (btrim(task) <> ''),
    provider_key text NOT NULL CHECK (provider_key IN ('STUDENT', 'TUTOR', 'ADMIN')),
    admitted_at timestamptz NOT NULL,
    reserved_tokens integer NOT NULL CHECK (reserved_tokens > 0),
    in_flight_until timestamptz
);
CREATE INDEX ai_attempt_actor_task_window ON ai_attempt(actor_id, task, admitted_at);
CREATE INDEX ai_attempt_key_window ON ai_attempt(provider_key, admitted_at);
CREATE INDEX ai_attempt_key_inflight ON ai_attempt(provider_key, in_flight_until)
    WHERE in_flight_until IS NOT NULL;
