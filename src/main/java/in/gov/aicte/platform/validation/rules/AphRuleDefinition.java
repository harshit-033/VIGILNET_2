package in.gov.aicte.platform.validation.rules;

import java.util.List;

public record AphRuleDefinition(
        String ruleId,
        String ruleName,
        String aphClause,
        String ruleCategory, // STATUTORY_MANDATORY or ADVISORY_RECOMMENDED
        String decisionType, // DETERMINISTIC_RULE or AI_ASSISTED_ADVISORY
        String fieldReference,
        List<String> dependentFields,
        boolean isTimeDependent,
        String description
) {}
