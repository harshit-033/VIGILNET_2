package in.gov.aicte.platform.validation.dto;

import in.gov.aicte.platform.validation.ValidationProvenance;
import java.time.Instant;

public record ValidationProvenanceResponse(
        Long id,
        String ruleId,
        String ruleName,
        String aphClause,
        String ruleCategory,
        String fieldReference,
        String inputPayloadHash,
        String validationOutcome,
        String decisionType,
        String reEvaluationReason,
        Instant evaluatedAt,
        String evaluatedBy,
        Long executionDurationMs,
        String proofDetails,
        Boolean isValid
) {
    public static ValidationProvenanceResponse from(ValidationProvenance vp) {
        return new ValidationProvenanceResponse(
                vp.getId(),
                vp.getRuleId(),
                vp.getRuleName(),
                vp.getAphClause(),
                vp.getRuleCategory(),
                vp.getFieldReference(),
                vp.getInputPayloadHash(),
                vp.getValidationOutcome(),
                vp.getDecisionType(),
                vp.getReEvaluationReason(),
                vp.getEvaluatedAt(),
                vp.getEvaluatedBy(),
                vp.getExecutionDurationMs(),
                vp.getProofDetails(),
                vp.getIsValid()
        );
    }
}
