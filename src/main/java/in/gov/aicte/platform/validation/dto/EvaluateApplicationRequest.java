package in.gov.aicte.platform.validation.dto;

import java.util.Map;

public record EvaluateApplicationRequest(
        Map<String, String> modifiedFields,
        Boolean forceFullEvaluation,
        String actorName,
        String actorRole
) {
    public EvaluateApplicationRequest {
        if (modifiedFields == null) {
            modifiedFields = Map.of();
        }
        if (forceFullEvaluation == null) {
            forceFullEvaluation = false;
        }
    }
}
