package in.gov.aicte.platform.evc.dto;

public record CompleteVisitRequest(
        String completionNotes,
        String actorName,
        String actorRole
) {
}
