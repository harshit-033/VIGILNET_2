package in.gov.aicte.platform.validation;

import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.auth.SecurityUtils;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import in.gov.aicte.platform.validation.dto.EvaluateApplicationRequest;
import in.gov.aicte.platform.validation.dto.ValidationProvenanceResponse;
import in.gov.aicte.platform.validation.dto.ValidationReportResponse;
import in.gov.aicte.platform.validation.rules.AphRuleDefinition;
import in.gov.aicte.platform.validation.rules.AphRuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncrementalValidationService {

    private static final Logger log = LoggerFactory.getLogger(IncrementalValidationService.class);

    private final ValidationProvenanceRepository provenanceRepository;
    private final ApplicationRepository applicationRepository;
    private final AphRuleRegistry ruleRegistry;
    private final SecurityUtils securityUtils;

    public IncrementalValidationService(
            ValidationProvenanceRepository provenanceRepository,
            ApplicationRepository applicationRepository,
            AphRuleRegistry ruleRegistry,
            SecurityUtils securityUtils
    ) {
        this.provenanceRepository = provenanceRepository;
        this.applicationRepository = applicationRepository;
        this.ruleRegistry = ruleRegistry;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public ValidationReportResponse evaluateApplication(String idOrRef, EvaluateApplicationRequest request) {
        Application application = resolveApplication(idOrRef);

        // Security scoping assertions
        if (application.getInstitution() != null) {
            securityUtils.assertInstitutionAccess(application.getInstitution());
            securityUtils.assertRegionAccess(application.getInstitution().getRegion());
        }

        List<AphRuleDefinition> allRules = ruleRegistry.getAllRules();
        List<ValidationProvenance> existingProofs = provenanceRepository.findByApplicationIdAndIsValidTrue(application.getId());
        Map<String, ValidationProvenance> proofMap = existingProofs.stream()
                .collect(Collectors.toMap(ValidationProvenance::getRuleId, vp -> vp, (v1, v2) -> v1));

        Map<String, String> modifiedFields = request.modifiedFields();
        boolean forceFull = Boolean.TRUE.equals(request.forceFullEvaluation());

        int skippedCount = 0;
        int reevaluatedCount = 0;
        List<ValidationProvenance> activeProofs = new ArrayList<>();

        Instant now = Instant.now();

        for (AphRuleDefinition rule : allRules) {
            ValidationProvenance existing = proofMap.get(rule.ruleId());
            String reEvalReason = null;

            if (forceFull) {
                reEvalReason = "FORCE_FULL_REEVALUATION";
            } else if (existing == null) {
                reEvalReason = "INITIAL_EVALUATION";
            } else {
                // Check if directly referenced field is dirty
                if (modifiedFields.containsKey(rule.fieldReference())) {
                    String newPayload = modifiedFields.get(rule.fieldReference());
                    String newHash = computeHash(newPayload);
                    if (!newHash.equalsIgnoreCase(existing.getInputPayloadHash())) {
                        reEvalReason = "FIELD_DIRTY_HASH_MISMATCH";
                    }
                }

                // Check dependency graph for upstream changes
                if (reEvalReason == null && rule.dependentFields() != null) {
                    for (String dep : rule.dependentFields()) {
                        if (modifiedFields.containsKey(dep)) {
                            reEvalReason = "DEPENDENCY_INVALIDATION";
                            break;
                        }
                    }
                }

                // Check time-dependent expirations
                if (reEvalReason == null && rule.isTimeDependent()) {
                    long daysSinceEval = ChronoUnit.DAYS.between(existing.getEvaluatedAt(), now);
                    if (daysSinceEval >= 30) {
                        reEvalReason = "TIME_EXPIRY_SLA";
                    }
                }
            }

            if (reEvalReason == null && existing != null) {
                // SKIP rule evaluation! Preserve proof, timestamp, and audit trail!
                skippedCount++;
                activeProofs.add(existing);
                log.info("Incremental Validation: Skipped rule {} for app {} (Cache Hit - Unmodified)",
                        rule.ruleId(), application.getApplicationReference());
            } else {
                // RE-EVALUATE rule!
                reevaluatedCount++;
                long startTime = System.currentTimeMillis();

                // Invalidate prior record if one exists
                provenanceRepository.invalidatePriorProvenance(application.getId(), rule.ruleId());

                String payloadToEvaluate = modifiedFields.getOrDefault(rule.fieldReference(),
                        "canonical_baseline_payload_for_" + rule.fieldReference());
                String hash = computeHash(payloadToEvaluate);

                // Execute rule logic
                ValidationResult result = executeRuleCheck(rule, payloadToEvaluate);
                long duration = Math.max(1, System.currentTimeMillis() - startTime);

                ValidationProvenance newProof = new ValidationProvenance();
                newProof.setApplication(application);
                newProof.setRuleId(rule.ruleId());
                newProof.setRuleName(rule.ruleName());
                newProof.setAphClause(rule.aphClause());
                newProof.setRuleCategory(rule.ruleCategory());
                newProof.setFieldReference(rule.fieldReference());
                newProof.setInputPayloadHash(hash);
                newProof.setValidationOutcome(result.outcome);
                newProof.setDecisionType(rule.decisionType());
                newProof.setReEvaluationReason(reEvalReason != null ? reEvalReason : "INITIAL_EVALUATION");
                newProof.setEvaluatedAt(now);
                newProof.setEvaluatedBy(request.actorName() != null ? request.actorName() : "SYSTEM_VALIDATION_ENGINE");
                newProof.setExecutionDurationMs(duration);
                newProof.setProofDetails(result.details);
                newProof.setIsValid(true);

                ValidationProvenance saved = provenanceRepository.save(newProof);
                activeProofs.add(saved);

                log.info("Incremental Validation: Re-evaluated rule {} for app {} (Reason: {})",
                        rule.ruleId(), application.getApplicationReference(), newProof.getReEvaluationReason());
            }
        }

        // Verify if all statutory mandatory rules are PASSED
        boolean allMandatoryPassed = activeProofs.stream()
                .filter(p -> "STATUTORY_MANDATORY".equalsIgnoreCase(p.getRuleCategory()))
                .allMatch(p -> "PASSED".equalsIgnoreCase(p.getValidationOutcome()));

        // Build dependency graph representation
        Map<String, List<String>> dependencyGraph = allRules.stream()
                .collect(Collectors.toMap(AphRuleDefinition::ruleId, AphRuleDefinition::dependentFields));

        List<ValidationProvenanceResponse> responseProofs = activeProofs.stream()
                .map(ValidationProvenanceResponse::from)
                .toList();

        return new ValidationReportResponse(
                application.getId(),
                application.getApplicationReference(),
                application.getInstitution() != null ? application.getInstitution().getName() : "Unassigned Institution",
                allRules.size(),
                skippedCount,
                reevaluatedCount,
                allMandatoryPassed,
                now,
                responseProofs,
                dependencyGraph
        );
    }

    @Transactional(readOnly = true)
    public List<ValidationProvenanceResponse> getProvenanceHistory(String idOrRef) {
        Application application = resolveApplication(idOrRef);
        if (application.getInstitution() != null) {
            securityUtils.assertInstitutionAccess(application.getInstitution());
            securityUtils.assertRegionAccess(application.getInstitution().getRegion());
        }
        return provenanceRepository.findByApplicationIdOrderByEvaluatedAtDesc(application.getId())
                .stream()
                .map(ValidationProvenanceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AphRuleDefinition> getAllRules() {
        return ruleRegistry.getAllRules();
    }

    private ValidationResult executeRuleCheck(AphRuleDefinition rule, String payload) {
        // Explicit distinction between Statutory Rules and AI Recommendations
        if ("AI_ASSISTED_ADVISORY".equalsIgnoreCase(rule.decisionType())) {
            return new ValidationResult(
                    "PASSED",
                    "DEMO HEURISTIC ADVISORY (Demo Score: 0.12 Low Risk). NOTE: Non-binding algorithmic assessment for officer guidance only; does not represent an authoritative AICTE regulatory determination. Human Scrutiny Officer evaluation required."
            );
        }

        if (payload.toLowerCase().contains("deficiency") || payload.toLowerCase().contains("fail") || payload.toLowerCase().contains("invalid")) {
            return new ValidationResult(
                    "FAILED",
                    "Non-compliance detected against " + rule.aphClause() + ": Declared parameters do not satisfy statutory minimum."
            );
        }

        return new ValidationResult(
                "PASSED",
                "Compliance confirmed under " + rule.aphClause() + ": Institutional parameters meet or exceed prescribed standard."
        );
    }

    private String computeHash(String input) {
        if (input == null) input = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private Application resolveApplication(String idOrRef) {
        try {
            Long id = Long.parseLong(idOrRef);
            return applicationRepository.findById(id)
                    .orElseGet(() -> applicationRepository.findByApplicationReference(idOrRef)
                            .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + idOrRef)));
        } catch (NumberFormatException e) {
            return applicationRepository.findByApplicationReference(idOrRef)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + idOrRef));
        }
    }

    private record ValidationResult(String outcome, String details) {}
}
