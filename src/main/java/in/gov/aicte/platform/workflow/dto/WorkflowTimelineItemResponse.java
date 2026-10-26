package in.gov.aicte.platform.workflow.dto;

import in.gov.aicte.platform.workflow.WorkflowTransition;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record WorkflowTimelineItemResponse(
        String id,
        String title,
        String actor,
        String timestamp,
        String description
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            .withZone(ZoneId.of("UTC"));

    public static WorkflowTimelineItemResponse from(WorkflowTransition transition) {
        String actorDisplay = (transition.getActorName() != null && !transition.getActorName().isBlank())
                ? String.format("%s (%s)", transition.getActorName(), transition.getActorRole() != null ? transition.getActorRole() : "Official")
                : transition.getActorId();

        String title = switch (transition.getAction()) {
            case "RETURN_FOR_CORRECTION" -> "Deficiency Notice Issued · Returned for Correction";
            case "SUBMIT_CORRECTIONS" -> "Corrections Resubmitted by Institution";
            case "RESOLVE_DEFICIENCY" -> "Deficiency Resolved by Scrutiny Desk";
            case "SUBMIT_APPLICATION" -> "Application Dossier Formally Registered";
            case "COMPLETE_SCRUTINY" -> String.format("Scrutiny Cleared · Forwarded to %s", transition.getToStage());
            default -> String.format("Workflow Action: %s", transition.getAction());
        };

        String desc = transition.getReason() != null ? transition.getReason() : "State transition completed.";

        return new WorkflowTimelineItemResponse(
                "TL-" + transition.getId(),
                title,
                actorDisplay,
                FORMATTER.format(transition.getCreatedAt()),
                desc
        );
    }
}
