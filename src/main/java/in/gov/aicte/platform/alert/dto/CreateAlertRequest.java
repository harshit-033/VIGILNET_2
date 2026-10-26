package in.gov.aicte.platform.alert.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateAlertRequest(
        Long institutionId,
        @NotBlank(message = "Alert type is required") String type,
        @NotBlank(message = "Alert category is required") String category,
        String severity,
        String urgency,
        @NotBlank(message = "Alert message is required") String message,
        String requiredAction,
        String relatedRecordId,
        String recipient,
        String channel,
        LocalDate dueDate
) {}
