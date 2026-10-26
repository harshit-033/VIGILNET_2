package in.gov.aicte.platform.compliance.dto;

public record ComplianceMetricsResponse(
        long totalObligations,
        long dueSoon,
        long overdue,
        long evidenceSubmitted,
        long verificationPending,
        long clarificationRequired,
        long verifiedCompliant
) {
}
