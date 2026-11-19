CREATE TABLE app_user (
                          id UUID PRIMARY KEY,
                          email VARCHAR(254) NOT NULL UNIQUE,
                          display_name VARCHAR(160) NOT NULL,
                          password_hash VARCHAR(100) NOT NULL,
                          enabled BOOLEAN NOT NULL DEFAULT TRUE,
                          password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
                          failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                          locked_until TIMESTAMPTZ,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user_role (
                               user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                               role VARCHAR(32) NOT NULL,
                               PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_session (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                 token_hash VARCHAR(64) NOT NULL UNIQUE,
                                 expires_at TIMESTAMPTZ NOT NULL,
                                 revoked_at TIMESTAMPTZ,
                                 replaced_by UUID,
                                 user_agent VARCHAR(500),
                                 ip_address VARCHAR(64),
                                 created_at TIMESTAMPTZ NOT NULL,
                                 last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_user
    ON refresh_session(user_id);

CREATE INDEX idx_refresh_expires
    ON refresh_session(expires_at);


CREATE TABLE audit_event (
                             id UUID PRIMARY KEY,
                             actor_id UUID,
                             actor_email VARCHAR(254),
                             event_type VARCHAR(80) NOT NULL,
                             entity_type VARCHAR(80),
                             entity_id UUID,
                             summary VARCHAR(1000) NOT NULL,
                             correlation_id VARCHAR(100),
                             ip_address VARCHAR(64),
                             user_agent VARCHAR(500),
                             occurred_at TIMESTAMPTZ NOT NULL,
                             details_json TEXT
);

CREATE INDEX idx_audit_occurred
    ON audit_event(occurred_at DESC);

CREATE INDEX idx_audit_actor
    ON audit_event(actor_email);

CREATE INDEX idx_audit_entity
    ON audit_event(entity_type, entity_id);

CREATE UNIQUE INDEX uq_user_email_ci ON app_user(lower(email));
ALTER TABLE app_user ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE app_user_role ADD CONSTRAINT role_allowed CHECK(role IN ('VIEWER','OPERATOR','ADMIN'));

CREATE TABLE exam_discipline (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(120) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_discipline_name_ci ON exam_discipline(lower(name));
INSERT INTO exam_discipline(code,name)
 SELECT 'LEGACY-' || row_number() OVER(ORDER BY lower(exam_name)), min(exam_name)
 FROM exam_applications GROUP BY lower(exam_name);

ALTER TABLE applicants ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE applicants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE examiners ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE examiners ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE exam_applications ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE exam_applications ADD COLUMN external_id VARCHAR(100);
UPDATE exam_applications SET external_id = 'EX-' || lpad(id::text, 6, '0');
ALTER TABLE exam_applications ALTER COLUMN external_id SET NOT NULL;
ALTER TABLE exam_applications ADD CONSTRAINT uq_application_external UNIQUE(external_id);
ALTER TABLE exam_applications ADD COLUMN discipline_id UUID REFERENCES exam_discipline(id);
UPDATE exam_applications a SET discipline_id=d.id FROM exam_discipline d WHERE lower(a.exam_name)=lower(d.name);
ALTER TABLE exam_applications ALTER COLUMN discipline_id SET NOT NULL;
ALTER TABLE exam_applications ADD COLUMN preparation_percent INTEGER NOT NULL DEFAULT 0 CHECK(preparation_percent BETWEEN 0 AND 100);
ALTER TABLE exam_applications ADD COLUMN documents_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE exam_applications ADD COLUMN risk_level VARCHAR(32) NOT NULL DEFAULT 'CRITICAL' CHECK(risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL','NOT_APPLICABLE'));
UPDATE exam_applications SET risk_level='NOT_APPLICABLE' WHERE status IN ('COMPLETED','CANCELLED');
ALTER TABLE exam_applications ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE exam_applications ADD COLUMN archived_at TIMESTAMPTZ;
ALTER TABLE exam_applications ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE exam_applications ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_application_discipline ON exam_applications(discipline_id);
CREATE INDEX idx_application_archive_risk ON exam_applications(archived,risk_level);
CREATE INDEX idx_application_external_search ON exam_applications(lower(external_id));
CREATE INDEX idx_applicant_name_search ON applicants(lower(full_name));
CREATE INDEX idx_application_preparation ON exam_applications(preparation_percent);
DROP INDEX uq_active_exam_slot;
CREATE UNIQUE INDEX uq_active_exam_slot ON exam_applications(applicant_id,lower(exam_name),scheduled_at) WHERE status IN ('NEW','APPROVED') AND NOT archived;
CREATE TABLE exam_application_tag (
 application_id BIGINT NOT NULL REFERENCES exam_applications(id) ON DELETE CASCADE,
 tag VARCHAR(50) NOT NULL, PRIMARY KEY(application_id,tag)
);
CREATE INDEX idx_application_tag ON exam_application_tag(tag);
CREATE TABLE exam_application_note (
 id UUID PRIMARY KEY, application_id BIGINT NOT NULL REFERENCES exam_applications(id) ON DELETE CASCADE,
 author_id UUID NOT NULL REFERENCES app_user(id), body VARCHAR(4000) NOT NULL,
 pinned BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_application_note ON exam_application_note(application_id,pinned DESC,created_at DESC);
CREATE TABLE csv_import_preview (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL REFERENCES app_user(id), file_hash VARCHAR(64) NOT NULL,
 mode VARCHAR(20) NOT NULL, expires_at TIMESTAMPTZ NOT NULL, applied_at TIMESTAMPTZ,
 content TEXT NOT NULL
);
CREATE INDEX idx_preview_expiry ON csv_import_preview(expires_at);
