package in.gov.aicte.platform.evc.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.evc.EvcVisit;
import in.gov.aicte.platform.institution.Institution;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record EvcResponse(
        String id,
        String evcReference,
        Long backendId,
        String applicationId,
        Long applicationBackendId,
        String institutionId,
        String institutionName,
        String region,
        String inspectionType,
        String status,
        String urgency,
        LocalDate visitDate,
        String coordinator,
        String coordinatorId,
        LocalDate deadline,
        List<String> experts,
        String reportStatus,
        String reportOutcome,
        String reportNotes,
        String completionNotes,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static EvcResponse from(EvcVisit visit) {
        List<String> expertsList = Collections.emptyList();
        if (visit.getExpertsJson() != null && !visit.getExpertsJson().isBlank()) {
            try {
                expertsList = OBJECT_MAPPER.readValue(visit.getExpertsJson(), new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }

        Application app = visit.getApplication();
        Institution inst = app != null ? app.getInstitution() : null;

        String appId = app != null ? (app.getApplicationReference() != null ? app.getApplicationReference() : ("APP-" + app.getId())) : "";
        Long appBackendId = app != null ? app.getId() : null;
        String instId = inst != null ? (inst.getInstitutionalIdentifier() != null ? inst.getInstitutionalIdentifier() : ("INST-" + inst.getId())) : "";
        String instName = inst != null ? inst.getName() : "Unknown Institution";
        String reg = inst != null ? inst.getRegion() : "Northern Region";

        String evcId = visit.getEvcReference() != null ? visit.getEvcReference() : ("EVC-" + visit.getId());

        return new EvcResponse(
                evcId,
                visit.getEvcReference(),
                visit.getId(),
                appId,
                appBackendId,
                instId,
                instName,
                reg,
                visit.getInspectionType(),
                visit.getStatus(),
                visit.getUrgency(),
                visit.getVisitDate(),
                visit.getCoordinatorName(),
                visit.getCoordinatorId(),
                visit.getDeadline(),
                expertsList,
                visit.getReportStatus(),
                visit.getReportOutcome(),
                visit.getReportNotes(),
                visit.getCompletionNotes(),
                visit.getVersion(),
                visit.getCreatedAt(),
                visit.getUpdatedAt()
        );
    }
}
