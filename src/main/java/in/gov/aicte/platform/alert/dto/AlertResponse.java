package in.gov.aicte.platform.alert.dto;

import in.gov.aicte.platform.alert.PlatformAlert;
import java.time.Instant;
import java.time.LocalDate;

public record AlertResponse(
        String id,
        Long backendId,
        String alertReference,
        Long institutionId,
        String institutionalIdentifier,
        String institutionName,
        String type,
        String category,
        String severity,
        String urgency,
        String message,
        String requiredAction,
        String relatedRecordId,
        String recipient,
        String channel,
        String deliveryStatus,
        Boolean acknowledged,
        String acknowledgedBy,
        Instant acknowledgedAt,
        Boolean escalated,
        String escalatedTo,
        String escalationReason,
        LocalDate dueDate,
        Instant createdAt
) {
    public static AlertResponse from(PlatformAlert a) {
        String instIdent = (a.getInstitution() != null) ? a.getInstitution().getInstitutionalIdentifier() : null;
        String instName = (a.getInstitution() != null) ? a.getInstitution().getName() : null;
        Long instId = (a.getInstitution() != null) ? a.getInstitution().getId() : null;

        return new AlertResponse(
                a.getAlertReference(),
                a.getId(),
                a.getAlertReference(),
                instId,
                instIdent,
                instName,
                a.getType(),
                a.getCategory(),
                a.getSeverity(),
                a.getUrgency(),
                a.getMessage(),
                a.getRequiredAction(),
                a.getRelatedRecordId(),
                a.getRecipient(),
                a.getChannel(),
                a.getDeliveryStatus(),
                a.getAcknowledged(),
                a.getAcknowledgedBy(),
                a.getAcknowledgedAt(),
                a.getEscalated(),
                a.getEscalatedTo(),
                a.getEscalationReason(),
                a.getDueDate(),
                a.getCreatedAt()
        );
    }
}
