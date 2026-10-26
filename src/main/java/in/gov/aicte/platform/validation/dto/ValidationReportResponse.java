package in.gov.aicte.platform.validation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ValidationReportResponse(
        Long applicationId,
        String applicationReference,
        String institutionName,
        Integer totalRulesCount,
        Integer rulesSkippedCount,
        Integer rulesReevaluatedCount,
        Boolean statutoryMandatoryPassed,
        Instant evaluationTimestamp,
        List<ValidationProvenanceResponse> activeProofs,
        Map<String, List<String>> dependencyGraph
) {}
