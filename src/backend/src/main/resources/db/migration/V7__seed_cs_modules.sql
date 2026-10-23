-- Same module list as MP2's DemoData: named requirements in the NUS BComp (CS)
-- AY2026/27 curriculum, https://www.comp.nus.edu.sg/cug/per-cohort/cs/cs-26-27/
-- IDs match MP2's UUID.nameUUIDFromBytes("nus-cs-2026-27:" + code).
-- Codes an admin already created are left unchanged.
INSERT INTO modules (id, code, name, active) VALUES
    ('f844f062-e84e-38a5-9482-9f629a9540bd', 'CS1101S', 'Programming Methodology', true),
    ('c9ba5da9-91f6-3183-bb6b-87b5e4dfdfa9', 'ES2660', 'Communicating in the Information Age', true),
    ('a214d753-ef1a-39ad-93ae-b4622404ed44', 'IS1108', 'Digital and AI Ethics', true),
    ('53e5e33e-bc10-3fb4-af68-6af38bcd2e61', 'CS1231S', 'Discrete Structures', true),
    ('6909fb9c-e342-304b-83e4-d486eadc5c5c', 'CS2030S', 'Programming Methodology II', true),
    ('145e4c55-6766-3639-9f2b-5b94edac7c65', 'CS2040S', 'Data Structures and Algorithms', true),
    ('d9f9cfdb-5815-3565-a2a5-a5f3552a8457', 'CS2100', 'Computer Organisation', true),
    ('8b7e6537-9c2d-3a06-a6e1-9d62170ea01a', 'CS2101', 'Effective Communication for Computing Professionals', true),
    ('7d262066-8490-31c9-bc9a-fddcfdd078e7', 'CS2103T', 'Software Engineering', true),
    ('0e50865a-6b03-39a5-9f65-a937c1137d06', 'CS2106', 'Introduction to Operating Systems', true),
    ('f66a4d64-5f85-38ba-a564-9e2a0b108655', 'CS2109S', 'Introduction to AI and Machine Learning', true),
    ('0e1e3665-091f-3e94-9349-9857b9655542', 'CS3230', 'Design and Analysis of Algorithms', true),
    ('c98cd72f-02b2-3818-a2bd-414083da9d5f', 'MA1521', 'Calculus for Computing', true),
    ('460c0738-54a3-3d66-88c5-e74324e8e4f6', 'MA1522', 'Linear Algebra for Computing', true),
    ('25ee5854-5846-39d5-9c64-2b2e0d2e0bcd', 'ST2334', 'Probability and Statistics', true)
ON CONFLICT DO NOTHING;
