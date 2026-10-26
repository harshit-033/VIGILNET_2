package in.gov.aicte.platform.application.submission.dto;

import in.gov.aicte.platform.application.submission.ApplicationSubmission;
import java.time.Instant;

public record SubmissionResponse(
        Long id,
        String submissionReference,
        String submissionType,
        Integer versionNumber,
        String submittedById,
        String submittedByName,
        String notes,
        String status,
        Instant createdAt
) {
    public static SubmissionResponse from(ApplicationSubmission sub) {
        return new SubmissionResponse(
                sub.getId(),
                sub.getSubmissionReference(),
                sub.getSubmissionType(),
                sub.getVersionNumber(),
                sub.getSubmittedById(),
                sub.getSubmittedByName(),
                sub.getNotes(),
                sub.getStatus(),
                sub.getCreatedAt()
        );
    }
}
