package in.gov.aicte.platform.application.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.checklist.dto.ChecklistItemResponse;
import in.gov.aicte.platform.application.deficiency.dto.DeficiencyResponse;
import in.gov.aicte.platform.application.submission.dto.SubmissionResponse;
import in.gov.aicte.platform.workflow.dto.WorkflowTimelineItemResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record ApplicationResponse(
        Long id,
        String applicationReference,
        String applicationType,
        String academicYear,
        String status,
        String currentStage,
        String ownerName,
        String ownerId,
        LocalDate dueDate,
        String observations,
        List<String> deficiencies,
        List<DeficiencyResponse> deficiencyItems,
        List<SubmissionResponse> submissions,
        List<ChecklistItemResponse> checklist,
        Long ageDays,
        Long version,
        InstitutionSummaryResponse institution,
        List<WorkflowTimelineItemResponse> timeline,
        Instant createdAt,
        Instant updatedAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ApplicationResponse from(Application app) {
        return from(app, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static ApplicationResponse from(Application app, List<WorkflowTimelineItemResponse> timeline) {
        return from(app, timeline, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static ApplicationResponse from(
            Application app,
            List<WorkflowTimelineItemResponse> timeline,
            List<DeficiencyResponse> deficiencyItems,
            List<SubmissionResponse> submissions,
            List<ChecklistItemResponse> checklist
    ) {
        List<String> deficienciesList = Collections.emptyList();
        if (deficiencyItems != null && !deficiencyItems.isEmpty()) {
            deficienciesList = deficiencyItems.stream()
                    .map(d -> d.deficiencyCode() + ": " + d.description() + " (" + d.status() + ")")
                    .toList();
        } else if (app.getDeficienciesJson() != null && !app.getDeficienciesJson().isBlank()) {
            try {
                deficienciesList = OBJECT_MAPPER.readValue(app.getDeficienciesJson(), new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }

        long ageDays = 0L;
        if (app.getCreatedAt() != null) {
            ageDays = Math.max(0L, Duration.between(app.getCreatedAt(), Instant.now()).toDays());
        }

        return new ApplicationResponse(
                app.getId(),
                app.getApplicationReference(),
                app.getApplicationType(),
                app.getAcademicYear(),
                app.getStatus(),
                app.getCurrentStage(),
                app.getOwnerName(),
                app.getOwnerId(),
                app.getDueDate(),
                app.getObservations(),
                deficienciesList,
                deficiencyItems != null ? deficiencyItems : Collections.emptyList(),
                submissions != null ? submissions : Collections.emptyList(),
                checklist != null ? checklist : Collections.emptyList(),
                ageDays,
                app.getVersion(),
                InstitutionSummaryResponse.from(app.getInstitution()),
                timeline != null ? timeline : Collections.emptyList(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
