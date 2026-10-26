package in.gov.aicte.platform.document.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyDocumentRequest(
        @NotBlank(message = "Verification status is required (e.g. Verified, Clarification requested)")
        String verificationStatus,

        String remarks,
        String actorName,
        String actorRole
) {
}
