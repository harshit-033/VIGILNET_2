-- =============================================================================
-- Flyway Migration V3: Expert Visit Committee (EVC) Operations Schema
-- Platform: AICTE Approval & Compliance Orchestration Platform (PS 26262)
-- Description: Creates evc_visits table, indexes, and backfills EVC work items
--              for all active applications at stage 'EVC scheduling'.
-- =============================================================================

CREATE TABLE evc_visits (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    evc_reference VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'Awaiting scheduling',
    urgency VARCHAR(32) NOT NULL DEFAULT 'In progress',
    inspection_type VARCHAR(64) NOT NULL DEFAULT 'Physical On-Site Verification',
    coordinator_name VARCHAR(128) NOT NULL DEFAULT 'C. Verma (EVC Operations)',
    coordinator_id VARCHAR(64) NOT NULL DEFAULT 'USR-EVC-01',
    visit_date DATE,
    deadline DATE,
    experts_json TEXT,
    report_status VARCHAR(128) NOT NULL DEFAULT 'Not Applicable',
    report_outcome VARCHAR(64),
    report_notes TEXT,
    completion_notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evc_visits_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT uk_evc_visits_reference UNIQUE (evc_reference),
    CONSTRAINT uk_evc_visits_application UNIQUE (application_id)
);

CREATE INDEX idx_evc_visits_status ON evc_visits (status);
CREATE INDEX idx_evc_visits_visit_date ON evc_visits (visit_date);
CREATE INDEX idx_evc_visits_app_id ON evc_visits (application_id);

-- Backfill EVC work items for existing applications currently at 'EVC scheduling' stage
INSERT INTO evc_visits (
    application_id,
    evc_reference,
    status,
    urgency,
    inspection_type,
    coordinator_name,
    coordinator_id,
    deadline,
    report_status
)
SELECT 
    a.id,
    'EVC-' || a.id,
    'Awaiting scheduling',
    CASE WHEN a.due_date IS NOT NULL AND a.due_date <= CURRENT_DATE THEN 'Overdue' ELSE 'In progress' END,
    'Physical On-Site Verification',
    COALESCE(a.owner_name, 'C. Verma (EVC Operations)'),
    COALESCE(a.owner_id, 'USR-EVC-01'),
    COALESCE(a.due_date, CURRENT_DATE + INTERVAL '14 days'),
    'Not Applicable'
FROM applications a
WHERE a.current_stage = 'EVC scheduling'
ON CONFLICT (application_id) DO NOTHING;
