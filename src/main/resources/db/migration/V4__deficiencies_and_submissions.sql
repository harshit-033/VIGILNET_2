-- =============================================================================
-- Flyway Migration V4: Deficiencies, Submission Versions, and Statutory Checklists
-- Platform: AICTE Approval & Compliance Orchestration Platform (PS 26262)
-- Description: Creates application_deficiencies, application_submissions, and
--              application_checklists tables for auditable scrutiny & correction.
-- =============================================================================

-- 1. Application Deficiencies Table
CREATE TABLE application_deficiencies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    deficiency_code VARCHAR(64) NOT NULL,
    requirement_reference VARCHAR(128),
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    institution_response TEXT,
    resolution_remarks TEXT,
    issued_by VARCHAR(128),
    resolved_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deficiencies_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE
);

CREATE INDEX idx_app_deficiencies_app_id ON application_deficiencies (application_id);
CREATE INDEX idx_app_deficiencies_status ON application_deficiencies (status);

-- 2. Application Submissions Table
CREATE TABLE application_submissions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    submission_reference VARCHAR(64) NOT NULL UNIQUE,
    submission_type VARCHAR(64) NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    submitted_by_id VARCHAR(128) NOT NULL,
    submitted_by_name VARCHAR(128),
    notes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_submissions_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE
);

CREATE INDEX idx_app_submissions_app_id ON application_submissions (application_id);

-- 3. Application Statutory Checklists Table
CREATE TABLE application_checklists (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    requirement_clause VARCHAR(128),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by VARCHAR(128),
    verified_at TIMESTAMPTZ,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_checklists_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT uk_checklists_app_item UNIQUE (application_id, item_code)
);

CREATE INDEX idx_app_checklists_app_id ON application_checklists (application_id);

-- 4. Seed Canonical Statutory Checklists for Existing Applications
INSERT INTO application_checklists (application_id, item_code, label, requirement_clause, is_verified, verified_by, verified_at)
SELECT a.id, 'CHK-LAND-01', 'Mandatory land title deed & non-encumbrance certificate verified', 'APH Table 1.1 Norms', TRUE, 'B. Singh (Scrutiny Officer)', a.created_at
FROM applications a
ON CONFLICT (application_id, item_code) DO NOTHING;

INSERT INTO application_checklists (application_id, item_code, label, requirement_clause, is_verified)
SELECT a.id, 'CHK-FACULTY-02', 'Faculty cadre ratio meets APH Table 2.1 norms (1:15 / 1:20)', 'APH Table 2.1 Norms', FALSE
FROM applications a
ON CONFLICT (application_id, item_code) DO NOTHING;

INSERT INTO application_checklists (application_id, item_code, label, requirement_clause, is_verified)
SELECT a.id, 'CHK-FIRE-03', 'Municipal building plan & fire safety NOC endorsement certified', 'APH Norms Clause 3.4', FALSE
FROM applications a
ON CONFLICT (application_id, item_code) DO NOTHING;

INSERT INTO application_checklists (application_id, item_code, label, requirement_clause, is_verified, verified_by, verified_at)
SELECT a.id, 'CHK-LAB-04', 'Laboratory and computing facilities verified compliant with AICTE norms', 'APH Norms Clause 4.2', TRUE, 'B. Singh (Scrutiny Officer)', a.created_at
FROM applications a
ON CONFLICT (application_id, item_code) DO NOTHING;

-- 5. Seed Initial Submissions for Existing Applications
INSERT INTO application_submissions (application_id, submission_reference, submission_type, version_number, submitted_by_id, submitted_by_name, notes, status, created_at)
SELECT a.id, 'SUB-' || a.id || '-v1', 'INITIAL_DOSSIER', 1, COALESCE(a.owner_id, 'USR-INST-01'), COALESCE(a.owner_name, 'Institution Representative'), 'Initial application package submitted with institutional disclosures.', 'SUBMITTED', a.created_at
FROM applications a
ON CONFLICT (submission_reference) DO NOTHING;
