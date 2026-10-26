package in.gov.aicte.platform.alert.dto;

import jakarta.validation.constraints.NotBlank;

public record EscalateAlertRequest(
        @NotBlank(message = "Escalation reason is required")
        String reason,

        String escalateTo,
        String actorName,
        String actorRole
) {
}
