-- =============================================================================
-- Flyway Migration V1: Initial Core Schema
-- Platform: AICTE Approval & Compliance Orchestration Platform (PS 26262)
-- Description: Core tables for institutions, applications, and audit events.
-- Note: Application types and status values are prototype values and subject
--       to confirmation against the official AICTE Approval Process Handbook.
-- =============================================================================

-- 1. Canonical Institutions Directory
CREATE TABLE institutions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institutional_identifier VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_institutions_identifier UNIQUE (institutional_identifier)
);

CREATE INDEX idx_institutions_name ON institutions (name);

-- 2. Applications (Approval Orchestration Entity)
CREATE TABLE applications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    application_reference VARCHAR(64) NOT NULL,
    application_type VARCHAR(64) NOT NULL,
    academic_year VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_applications_institution FOREIGN KEY (institution_id) 
        REFERENCES institutions (id) ON DELETE RESTRICT,
    CONSTRAINT uk_applications_reference UNIQUE (application_reference)
);

CREATE INDEX idx_applications_institution_id ON applications (institution_id);
CREATE INDEX idx_applications_status ON applications (status);
CREATE INDEX idx_applications_academic_year ON applications (academic_year);

-- 3. Audit Events (Traceable Immutable Audit History)
-- Captures consequential actions without storing secrets, passwords, or document bytes.
CREATE TABLE audit_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(128),
    target_entity_type VARCHAR(64) NOT NULL,
    target_entity_id VARCHAR(64) NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_target ON audit_events (target_entity_type, target_entity_id);
CREATE INDEX idx_audit_events_created_at ON audit_events (created_at);
CREATE INDEX idx_audit_events_event_type ON audit_events (event_type);
