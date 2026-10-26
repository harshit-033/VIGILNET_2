package in.gov.aicte.platform.compliance.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyComplianceRequest(
        @NotBlank(message = "Verification outcome is required (e.g. Verified compliant, Clarification requested)")
        String outcome,

        String remarks,
        String actorName,
        String actorRole
) {
}
