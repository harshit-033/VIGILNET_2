-- Platform: SAGE — System for Approval Governance and Evaluation (SIH 26262)
-- Migration V7: Real Compliance Evidence Storage & Alert Delivery Retry Tracking

-- 1. Extend compliance_obligations with durable storage path and media metadata
ALTER TABLE compliance_obligations
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(512),
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(128),
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;

-- 2. Extend platform_alerts with delivery attempt counts, timestamps, and error tracking
ALTER TABLE platform_alerts
    ADD COLUMN IF NOT EXISTS delivery_attempts INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS last_delivery_attempt_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_delivery_error TEXT;

-- 3. Dedicated audit table for alert delivery attempts and retry history
CREATE TABLE IF NOT EXISTS alert_delivery_attempts (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL REFERENCES platform_alerts(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    external_reference VARCHAR(128),
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_delivery_alert_id ON alert_delivery_attempts(alert_id);
