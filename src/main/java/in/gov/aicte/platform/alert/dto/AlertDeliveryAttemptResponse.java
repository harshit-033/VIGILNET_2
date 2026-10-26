package in.gov.aicte.platform.alert.dto;

import in.gov.aicte.platform.alert.AlertDeliveryAttempt;
import java.time.Instant;

public record AlertDeliveryAttemptResponse(
        Long id,
        String alertReference,
        Integer attemptNumber,
        String channel,
        String status,
        String errorMessage,
        String externalReference,
        Instant attemptedAt
) {
    public static AlertDeliveryAttemptResponse from(AlertDeliveryAttempt attempt) {
        return new AlertDeliveryAttemptResponse(
                attempt.getId(),
                attempt.getAlert() != null ? attempt.getAlert().getAlertReference() : null,
                attempt.getAttemptNumber(),
                attempt.getChannel(),
                attempt.getStatus(),
                attempt.getErrorMessage(),
                attempt.getExternalReference(),
                attempt.getAttemptedAt()
        );
    }
}
