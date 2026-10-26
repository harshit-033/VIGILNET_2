-- SAGE: persist OCR output with each registered document and version.
ALTER TABLE document_registry
    ADD COLUMN IF NOT EXISTS extracted_text TEXT,
    ADD COLUMN IF NOT EXISTS ocr_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS ocr_message TEXT;

ALTER TABLE document_versions
    ADD COLUMN IF NOT EXISTS extracted_text TEXT,
    ADD COLUMN IF NOT EXISTS ocr_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS ocr_message TEXT;

CREATE INDEX IF NOT EXISTS idx_document_registry_ocr_status ON document_registry(ocr_status);
