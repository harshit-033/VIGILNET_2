package in.gov.aicte.platform.compliance.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitComplianceEvidenceRequest(
        @NotBlank(message = "Evidence file reference or name is required")
        String evidenceFile,

        String evidenceHash,

        @NotBlank(message = "Notes explaining the submitted compliance proof are required")
        String notes,

        String actorName,
        String actorRole
) {
}
