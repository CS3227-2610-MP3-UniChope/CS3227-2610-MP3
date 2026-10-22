-- REVIEW-ONLY Tutor note schema for disposable test databases.
-- Do not move into production migrations without shared migration-path approval.
CREATE TABLE private_notes (
    booking_id uuid PRIMARY KEY REFERENCES bookings(id),
    -- Match Character.isWhitespace for Java String.strip(); NBSP/figure/narrow-NBSP are excluded.
    content text NOT NULL CHECK (length(btrim(content,
        chr(9)||chr(10)||chr(11)||chr(12)||chr(13)||' '||
        chr(28)||chr(29)||chr(30)||chr(31)||
        chr(5760)||chr(8192)||chr(8193)||chr(8194)||chr(8195)||chr(8196)||chr(8197)||chr(8198)||
        chr(8200)||chr(8201)||chr(8202)||chr(8232)||chr(8233)||chr(8287)||chr(12288)
    )) > 0),
    version bigint NOT NULL CHECK (version >= 1),
    updated_at timestamptz NOT NULL
);
