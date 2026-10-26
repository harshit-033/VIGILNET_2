package in.gov.aicte.platform.institution.dto;

import in.gov.aicte.platform.institution.Institution;

import java.time.Instant;

public record InstitutionResponse(
        Long id,
        String institutionalIdentifier,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static InstitutionResponse from(Institution institution) {
        return new InstitutionResponse(
                institution.getId(),
                institution.getInstitutionalIdentifier(),
                institution.getName(),
                institution.getCreatedAt(),
                institution.getUpdatedAt()
        );
    }
}
