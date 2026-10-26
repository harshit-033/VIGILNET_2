package in.gov.aicte.platform.evc.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitEvcReportRequest(
        @NotBlank(message = "Report outcome is mandatory (e.g. Recommended Approval, Recommended Rejection)")
        String reportOutcome,

        String reportNotes,
        String actorName,
        String actorRole
) {
}
