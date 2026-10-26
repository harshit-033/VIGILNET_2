package in.gov.aicte.platform.application.deficiency.dto;

import in.gov.aicte.platform.application.deficiency.ApplicationDeficiency;
import java.time.Instant;

public record DeficiencyResponse(
        Long id,
        String deficiencyCode,
        String requirementReference,
        String description,
        String status,
        String institutionResponse,
        String resolutionRemarks,
        String issuedBy,
        String resolvedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeficiencyResponse from(ApplicationDeficiency def) {
        return new DeficiencyResponse(
                def.getId(),
                def.getDeficiencyCode(),
                def.getRequirementReference(),
                def.getDescription(),
                def.getStatus(),
                def.getInstitutionResponse(),
                def.getResolutionRemarks(),
                def.getIssuedBy(),
                def.getResolvedBy(),
                def.getCreatedAt(),
                def.getUpdatedAt()
        );
    }
}
