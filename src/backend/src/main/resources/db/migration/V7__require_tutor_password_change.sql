ALTER TABLE users
    ADD COLUMN must_change_password boolean NOT NULL DEFAULT false;

UPDATE users
SET must_change_password = true
WHERE role = 'TUTOR';
