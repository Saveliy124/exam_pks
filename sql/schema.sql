CREATE TABLE IF NOT EXISTS applicants (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL CHECK (length(trim(full_name)) >= 2),
    email VARCHAR(254) NOT NULL UNIQUE CHECK (position('@' in email) > 1),
    phone VARCHAR(30) NOT NULL DEFAULT '',
    status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

-- Добавление поля в базу, созданную предыдущей версией проекта.
ALTER TABLE applicants ADD COLUMN IF NOT EXISTS status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'applicants'::regclass AND conname = 'applicants_status_check'
    ) THEN
        ALTER TABLE applicants ADD CONSTRAINT applicants_status_check
            CHECK (status IN ('ACTIVE', 'BLOCKED'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS examiners (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL CHECK (length(trim(full_name)) >= 2),
    email VARCHAR(254) NOT NULL UNIQUE CHECK (position('@' in email) > 1),
    subject VARCHAR(120) NOT NULL CHECK (length(trim(subject)) >= 2),
    status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE IF NOT EXISTS exam_applications (
    id BIGSERIAL PRIMARY KEY,
    applicant_id BIGINT NOT NULL REFERENCES applicants(id) ON DELETE RESTRICT,
    exam_name VARCHAR(120) NOT NULL CHECK (length(trim(exam_name)) >= 2),
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'NEW'
        CHECK (status IN ('NEW', 'APPROVED', 'COMPLETED', 'CANCELLED')),
    score SMALLINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_exam_score CHECK (
        (status = 'COMPLETED' AND score IS NOT NULL AND score BETWEEN 0 AND 100) OR
        (status <> 'COMPLETED' AND score IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_exam_slot
    ON exam_applications (applicant_id, lower(exam_name), scheduled_at)
    WHERE status IN ('NEW', 'APPROVED');

CREATE INDEX IF NOT EXISTS idx_exam_applications_date ON exam_applications(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_exam_applications_status ON exam_applications(status);
