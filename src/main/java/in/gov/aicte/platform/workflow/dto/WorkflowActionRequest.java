package in.gov.aicte.platform.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record WorkflowActionRequest(
        @JsonAlias({"action", "action_type"})
        @NotBlank(message = "Action type is required (e.g. SUBMIT_APPLICATION, RETURN_FOR_CORRECTION, SUBMIT_CORRECTIONS, RESOLVE_DEFICIENCY, COMPLETE_SCRUTINY)")
        String actionType,

        String actorName,

        String actorRole,

        String reason,

        String observations,

        List<String> newDeficiencies,

        String requirementReference,

        String nextStage,

        Long deficiencyId,

        String resolutionStatus,

        String resolutionRemarks,

        String checklistItemCode,

        Boolean checklistItemVerified
) {
    public WorkflowActionRequest(
            String actionType,
            String actorName,
            String actorRole,
            String reason,
            String observations,
            List<String> newDeficiencies,
            String nextStage
    ) {
        this(actionType, actorName, actorRole, reason, observations, newDeficiencies, null, nextStage, null, null, null, null, null);
    }
}
