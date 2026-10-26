package in.gov.aicte.platform.application.dto;

import in.gov.aicte.platform.institution.Institution;

public record InstitutionSummaryResponse(
        Long id,
        String institutionalIdentifier,
        String name,
        String region
) {
    public static InstitutionSummaryResponse from(Institution inst) {
        if (inst == null) return null;
        return new InstitutionSummaryResponse(inst.getId(), inst.getInstitutionalIdentifier(), inst.getName(), inst.getRegion());
    }
}
