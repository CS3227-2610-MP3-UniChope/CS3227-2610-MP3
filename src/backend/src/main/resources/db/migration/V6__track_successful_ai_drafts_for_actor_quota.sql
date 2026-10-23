-- A failed output may be retried without charging the actor-success quota.
-- Provider-key request/token budgets still count every admitted attempt.
ALTER TABLE ai_attempt
    ADD COLUMN actor_quota_counted boolean NOT NULL DEFAULT true;
