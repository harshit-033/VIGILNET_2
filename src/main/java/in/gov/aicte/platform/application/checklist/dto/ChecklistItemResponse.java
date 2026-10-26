package in.gov.aicte.platform.application.checklist.dto;

import in.gov.aicte.platform.application.checklist.ApplicationChecklist;
import java.time.Instant;

public record ChecklistItemResponse(
        Long id,
        String itemCode,
        String label,
        String requirementClause,
        Boolean isVerified,
        String verifiedBy,
        Instant verifiedAt,
        String remarks
) {
    public static ChecklistItemResponse from(ApplicationChecklist item) {
        return new ChecklistItemResponse(
                item.getId(),
                item.getItemCode(),
                item.getLabel(),
                item.getRequirementClause(),
                item.getVerified(),
                item.getVerifiedBy(),
                item.getVerifiedAt(),
                item.getRemarks()
        );
    }
}
