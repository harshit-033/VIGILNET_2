-- SIH 26262 - Phase 4: Compliance Obligations, Document Registry with SHA-256, and Alerts Management

CREATE TABLE compliance_obligations (
    id BIGSERIAL PRIMARY KEY,
    obligation_reference VARCHAR(64) UNIQUE NOT NULL,
    institution_id BIGINT NOT NULL REFERENCES institutions(id),
    category VARCHAR(128) NOT NULL,
    requirement TEXT NOT NULL,
    aph_clause VARCHAR(128),
    due_date DATE NOT NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'Pending submission',
    urgency VARCHAR(32) NOT NULL DEFAULT 'Normal',
    evidence_file VARCHAR(255),
    evidence_hash VARCHAR(64),
    evidence_notes TEXT,
    submitted_date DATE,
    submitted_by VARCHAR(128),
    reviewer VARCHAR(128),
    verification_outcome VARCHAR(64),
    verification_remarks TEXT,
    verified_date DATE,
    verified_by VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_compliance_institution ON compliance_obligations(institution_id);
CREATE INDEX idx_compliance_status ON compliance_obligations(status);
CREATE INDEX idx_compliance_due_date ON compliance_obligations(due_date);

CREATE TABLE document_registry (
    id BIGSERIAL PRIMARY KEY,
    document_reference VARCHAR(64) UNIQUE NOT NULL,
    institution_id BIGINT NOT NULL REFERENCES institutions(id),
    application_id BIGINT REFERENCES applications(id),
    document_type VARCHAR(128) NOT NULL,
    category VARCHAR(128) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    issue_date DATE,
    expiry_date DATE,
    validity_status VARCHAR(32) NOT NULL DEFAULT 'Valid',
    verification_status VARCHAR(64) NOT NULL DEFAULT 'Verification pending',
    reviewer VARCHAR(128),
    notes TEXT,
    version_number INT NOT NULL DEFAULT 1,
    uploaded_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doc_institution ON document_registry(institution_id);
CREATE INDEX idx_doc_application ON document_registry(application_id);
CREATE INDEX idx_doc_hash ON document_registry(sha256_hash);

CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES document_registry(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    notes TEXT,
    verification_status VARCHAR(64) NOT NULL,
    uploaded_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doc_version_parent ON document_versions(document_id);

CREATE TABLE platform_alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_reference VARCHAR(64) UNIQUE NOT NULL,
    institution_id BIGINT REFERENCES institutions(id),
    type VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'HIGH',
    urgency VARCHAR(32) NOT NULL DEFAULT 'Due soon',
    message TEXT NOT NULL,
    required_action TEXT,
    related_record_id VARCHAR(64),
    recipient VARCHAR(128),
    channel VARCHAR(32) DEFAULT 'PORTAL',
    delivery_status VARCHAR(32) DEFAULT 'Delivered',
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by VARCHAR(128),
    acknowledged_at TIMESTAMP,
    escalated BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_to VARCHAR(128),
    escalation_reason TEXT,
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_institution ON platform_alerts(institution_id);
CREATE INDEX idx_alert_urgency ON platform_alerts(urgency);
CREATE INDEX idx_alert_category ON platform_alerts(category);

-- Seed initial statutory compliance obligations for INST-1025
INSERT INTO compliance_obligations (
    obligation_reference, institution_id, category, requirement, aph_clause, due_date, status, urgency, reviewer
) VALUES
('OBL-101', 1, 'Anti-Ragging / Student Safety', 'Constitution of Anti-Ragging Committee and Squad with public display of emergency helpline numbers.', 'APH Appendix 6.1', CURRENT_DATE + INTERVAL '15 days', 'Pending submission', 'Due soon', 'D. Patel (Compliance Reviewer)'),
('OBL-102', 1, 'Statutory Committees', 'Notification of Internal Complaint Committee (ICC) as per AICTE (Gender Sensitization & Prevention of Sexual Harassment) Regulations.', 'APH Appendix 6.2', CURRENT_DATE + INTERVAL '30 days', 'Pending submission', 'Normal', 'D. Patel (Compliance Reviewer)'),
('OBL-103', 1, 'Faculty Biometrics', 'Implementation and integration of Aadhaar-enabled biometric attendance for all approved teaching faculty cadre.', 'APH Chapter II Clause 2.14', CURRENT_DATE - INTERVAL '3 days', 'Clarification requested', 'Overdue', 'D. Patel (Compliance Reviewer)'),
('OBL-104', 1, 'Mandatory Disclosure', 'Annual publishing of updated Mandatory Disclosure on the institution website with all faculty credentials and audited fee structure.', 'APH Appendix 10', CURRENT_DATE + INTERVAL '45 days', 'Verified compliant', 'Normal', 'D. Patel (Compliance Reviewer)');

-- Seed initial document registry records for INST-1025
INSERT INTO document_registry (
    document_reference, institution_id, application_id, document_type, category, file_name, storage_path, file_size_bytes, mime_type, sha256_hash, issue_date, expiry_date, validity_status, verification_status, reviewer, uploaded_by
) VALUES
('DOC-101', 1, 1, 'State Fire Service Safety NOC', 'Safety & Building NOCs', 'state_fire_noc_2026.pdf', 'uploads/state_fire_noc_2026.pdf', 2457600, 'application/pdf', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', CURRENT_DATE - INTERVAL '60 days', CURRENT_DATE + INTERVAL '700 days', 'Valid', 'Verified', 'B. Singh (Scrutiny Officer)', 'Dr. Ramesh Kumar'),
('DOC-102', 1, 1, 'Building Plan Approval & Occupancy Certificate', 'Land & Infrastructure', 'building_occupancy_cert.pdf', 'uploads/building_occupancy_cert.pdf', 5242880, 'application/pdf', '7d5be7e3848ff47e700a424e8e89f71422778536cc3e4777d130e527d4ee7f31', CURRENT_DATE - INTERVAL '180 days', CURRENT_DATE + INTERVAL '15 days', 'Due soon', 'Expiring soon', 'B. Singh (Scrutiny Officer)', 'Dr. Ramesh Kumar'),
('DOC-103', 1, 1, 'Affiliation Certificate from Examining University', 'Academic Affiliation', 'univ_affiliation_2026.pdf', 'uploads/univ_affiliation_2026.pdf', 1843200, 'application/pdf', 'bc6e8b4e7b89793542247f12e8b83777d853ba54ff31bb9962e21245b78f4a12', CURRENT_DATE - INTERVAL '300 days', CURRENT_DATE - INTERVAL '10 days', 'Overdue', 'Expired / no verified renewal', 'B. Singh (Scrutiny Officer)', 'Dr. Ramesh Kumar');

-- Seed initial document versions
INSERT INTO document_versions (document_id, version_number, file_name, storage_path, file_size_bytes, sha256_hash, verification_status, uploaded_by)
VALUES
(1, 1, 'state_fire_noc_2026.pdf', 'uploads/state_fire_noc_2026.pdf', 2457600, 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'Verified', 'Dr. Ramesh Kumar'),
(2, 1, 'building_occupancy_cert.pdf', 'uploads/building_occupancy_cert.pdf', 5242880, '7d5be7e3848ff47e700a424e8e89f71422778536cc3e4777d130e527d4ee7f31', 'Expiring soon', 'Dr. Ramesh Kumar'),
(3, 1, 'univ_affiliation_2026.pdf', 'uploads/univ_affiliation_2026.pdf', 1843200, 'bc6e8b4e7b89793542247f12e8b83777d853ba54ff31bb9962e21245b78f4a12', 'Expired / no verified renewal', 'Dr. Ramesh Kumar');

-- Seed initial platform alerts
INSERT INTO platform_alerts (
    alert_reference, institution_id, type, category, severity, urgency, message, required_action, related_record_id, recipient, due_date
) VALUES
('ALT-101', 1, 'EVC Inspection 48-Hour Report SLA', 'EVC Reminders', 'HIGH', 'Due soon', 'Physical EVC inspection concluded. Inspection panel report submission is pending within the 48-hour statutory turnaround window.', 'Upload signed inspection committee report and recommendation', 'EVC-1', 'C. Verma (EVC Operations)', CURRENT_DATE + INTERVAL '2 days'),
('ALT-102', 1, 'Deficiency Rectification Deadline', 'Upcoming Deadlines', 'HIGH', 'Due soon', 'Application returned for correction with statutory deficiencies. Institution response is due within 15 calendar days.', 'Submit itemized rectification package via institutional portal', 'APP-1025-2026', 'Dr. Ramesh Kumar', CURRENT_DATE + INTERVAL '7 days'),
('ALT-103', 1, 'Statutory NOC Expiry Alert', 'Overdue Tasks', 'CRITICAL', 'Overdue', 'University Affiliation Certificate expired on ' || (CURRENT_DATE - INTERVAL '10 days')::text || '. Mandatory renewal proof must be uploaded to avoid penal intake reduction.', 'Upload examining university renewal affiliation certificate', 'DOC-103', 'Dr. Ramesh Kumar', CURRENT_DATE - INTERVAL '10 days'),
('ALT-104', 1, 'Faculty Biometric Compliance Deficiency', 'Compliance Alerts', 'MEDIUM', 'Overdue', 'Clarification requested regarding biometric attendance integration for mechanical engineering faculty cadre.', 'Provide server log export and compliance affidavit', 'OBL-103', 'D. Patel (Compliance Reviewer)', CURRENT_DATE - INTERVAL '3 days');
