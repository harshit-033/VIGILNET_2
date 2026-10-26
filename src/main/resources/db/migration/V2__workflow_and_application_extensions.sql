-- =============================================================================
-- Flyway Migration V2: Additive Workflow Engine and Application Extensions
-- Platform: AICTE Approval & Compliance Orchestration Platform (PS 26262)
-- Description: Extends applications and institutions with workflow attributes,
--              creates workflow_transitions table, and seeds demo records.
-- Note: V1 schema is strictly preserved without modification.
-- =============================================================================

-- 1. Extend institutions with administrative region
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS region VARCHAR(64);

-- 2. Extend applications with workflow lifecycle attributes
ALTER TABLE applications ADD COLUMN IF NOT EXISTS current_stage VARCHAR(64) NOT NULL DEFAULT 'Scrutiny';
ALTER TABLE applications ADD COLUMN IF NOT EXISTS owner_name VARCHAR(128);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS owner_id VARCHAR(64);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS due_date DATE;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS observations TEXT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS deficiencies_json TEXT;

CREATE INDEX IF NOT EXISTS idx_applications_current_stage ON applications (current_stage);

-- 3. Workflow Transitions Table (Immutable Audit Trail of State Engine Transitions)
CREATE TABLE workflow_transitions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    from_stage VARCHAR(64) NOT NULL,
    to_stage VARCHAR(64) NOT NULL,
    from_status VARCHAR(32) NOT NULL,
    to_status VARCHAR(32) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    actor_name VARCHAR(128),
    actor_role VARCHAR(64),
    reason TEXT,
    observations TEXT,
    deficiencies_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workflow_transitions_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE
);

CREATE INDEX idx_workflow_transitions_app_id ON workflow_transitions (application_id);
CREATE INDEX idx_workflow_transitions_created_at ON workflow_transitions (created_at);

-- 4. Seed Canonical Demonstration Records for the SIH26262 Scrutiny Workflow
INSERT INTO institutions (institutional_identifier, name, region)
VALUES ('INST-1025', 'ABC Institute of Technology', 'Northern Region')
ON CONFLICT (institutional_identifier) DO UPDATE SET region = EXCLUDED.region;

INSERT INTO applications (institution_id, application_reference, application_type, academic_year, status, current_stage, owner_name, owner_id, due_date, observations, version)
SELECT id, 'APP-2026-1042', 'Extension of Approval / Compliance Review', '2026-2027', 'In progress', 'Scrutiny', 'B. Singh (Scrutiny Officer)', 'USR-SO-01', '2026-10-24', 'Land registration verified. Examining faculty cadre appointments.', 0
FROM institutions WHERE institutional_identifier = 'INST-1025'
ON CONFLICT (application_reference) DO NOTHING;
