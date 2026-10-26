package in.gov.aicte.platform.document.dto;

import in.gov.aicte.platform.document.DocumentVersion;
import java.time.Instant;

public record DocumentVersionResponse(
        Long id,
        Integer versionNumber,
        String fileName,
        Long fileSizeBytes,
        String sha256Hash,
        String notes,
        String verificationStatus,
        String uploadedBy,
        Instant createdAt
) {
    public static DocumentVersionResponse from(DocumentVersion v) {
        return new DocumentVersionResponse(
                v.getId(),
                v.getVersionNumber(),
                v.getFileName(),
                v.getFileSizeBytes(),
                v.getSha256Hash(),
                v.getNotes(),
                v.getVerificationStatus(),
                v.getUploadedBy(),
                v.getCreatedAt()
        );
    }
}
