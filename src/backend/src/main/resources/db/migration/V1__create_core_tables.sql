CREATE TABLE users (
    id uuid PRIMARY KEY,
    name text NOT NULL CHECK (btrim(name) <> ''),
    email text NOT NULL CHECK (btrim(email) <> ''),
    role text NOT NULL CHECK (role IN ('STUDENT', 'TUTOR', 'ADMIN')),
    active boolean NOT NULL
);
CREATE UNIQUE INDEX users_email_unique ON users (lower(btrim(email)));

CREATE TABLE modules (
    id uuid PRIMARY KEY,
    code text NOT NULL CHECK (btrim(code) <> ''),
    name text NOT NULL CHECK (btrim(name) <> ''),
    active boolean NOT NULL
);
CREATE UNIQUE INDEX modules_code_unique ON modules (lower(btrim(code)));

CREATE TABLE tutor_modules (
    tutor_id uuid NOT NULL REFERENCES users(id),
    module_id uuid NOT NULL REFERENCES modules(id),
    PRIMARY KEY (tutor_id, module_id)
);
CREATE INDEX tutor_modules_module ON tutor_modules (module_id);

CREATE TABLE slots (
    id uuid PRIMARY KEY,
    tutor_id uuid NOT NULL REFERENCES users(id),
    module_id uuid NOT NULL REFERENCES modules(id),
    start_time timestamptz NOT NULL,
    end_time timestamptz NOT NULL,
    status text NOT NULL CHECK (status IN ('AVAILABLE', 'BOOKED', 'CANCELLED', 'COMPLETED')),
    CHECK (end_time > start_time)
);
CREATE INDEX slots_tutor_time ON slots (tutor_id, start_time, id);
CREATE INDEX slots_module_time ON slots (module_id, start_time, id);

CREATE TABLE bookings (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES users(id),
    slot_id uuid NOT NULL REFERENCES slots(id),
    created_at timestamptz NOT NULL,
    status text NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED'))
);
CREATE UNIQUE INDEX bookings_one_active_per_slot ON bookings (slot_id) WHERE status = 'ACTIVE';
CREATE INDEX bookings_student ON bookings (student_id, created_at, id);
CREATE INDEX bookings_slot ON bookings (slot_id, created_at, id);
