package in.gov.aicte.platform.document.dto;

import in.gov.aicte.platform.document.DocumentRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DocumentResponse(
        String id,
        Long backendId,
        String documentReference,
        Long institutionId,
        String institutionalIdentifier,
        String institutionName,
        Long applicationId,
        String applicationReference,
        String documentType,
        String category,
        String fileName,
        Long fileSizeBytes,
        String fileSize,
        String mimeType,
        String fileHash,
        LocalDate issueDate,
        LocalDate expiryDate,
        String validityStatus,
        String verificationStatus,
        String reviewer,
        String notes,
        Integer versionNumber,
        String uploadedBy,
        Instant createdAt,
        List<DocumentVersionResponse> versions,
        String ocrStatus,
        String ocrMessage,
        String extractedText
) {
    public static DocumentResponse from(DocumentRecord doc) {
        String instIdent = (doc.getInstitution() != null) ? doc.getInstitution().getInstitutionalIdentifier() : null;
        String instName = (doc.getInstitution() != null) ? doc.getInstitution().getName() : null;
        Long instId = (doc.getInstitution() != null) ? doc.getInstitution().getId() : null;

        Long appId = (doc.getApplication() != null) ? doc.getApplication().getId() : null;
        String appRef = (doc.getApplication() != null) ? doc.getApplication().getApplicationReference() : null;

        String formattedSize = formatFileSize(doc.getFileSizeBytes());

        List<DocumentVersionResponse> versionList = (doc.getVersions() != null)
                ? doc.getVersions().stream().map(DocumentVersionResponse::from).toList()
                : List.of();

        return new DocumentResponse(
                doc.getDocumentReference(),
                doc.getId(),
                doc.getDocumentReference(),
                instId,
                instIdent,
                instName,
                appId,
                appRef,
                doc.getDocumentType(),
                doc.getCategory(),
                doc.getFileName(),
                doc.getFileSizeBytes(),
                formattedSize,
                doc.getMimeType(),
                doc.getSha256Hash(),
                doc.getIssueDate(),
                doc.getExpiryDate(),
                doc.getValidityStatus(),
                doc.getVerificationStatus(),
                doc.getReviewer(),
                doc.getNotes(),
                doc.getVersionNumber(),
                doc.getUploadedBy(),
                doc.getCreatedAt(),
                versionList,
                doc.getOcrStatus(),
                doc.getOcrMessage(),
                doc.getExtractedText()
        );
    }

    private static String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
