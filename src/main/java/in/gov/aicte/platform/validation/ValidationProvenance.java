package in.gov.aicte.platform.validation;

import in.gov.aicte.platform.application.Application;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "validation_provenance")
public class ValidationProvenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "aph_clause", nullable = false, length = 128)
    private String aphClause;

    @Column(name = "rule_category", nullable = false, length = 32)
    private String ruleCategory; // STATUTORY_MANDATORY, ADVISORY_RECOMMENDED

    @Column(name = "field_reference", nullable = false, length = 128)
    private String fieldReference;

    @Column(name = "input_payload_hash", nullable = false, length = 64)
    private String inputPayloadHash;

    @Column(name = "validation_outcome", nullable = false, length = 32)
    private String validationOutcome; // PASSED, FAILED, WARNING

    @Column(name = "decision_type", nullable = false, length = 32)
    private String decisionType; // DETERMINISTIC_RULE, AI_ASSISTED_ADVISORY

    @Column(name = "re_evaluation_reason", nullable = false, length = 64)
    private String reEvaluationReason; // INITIAL_EVALUATION, FIELD_DIRTY_HASH_MISMATCH, DEPENDENCY_INVALIDATION, TIME_EXPIRY_SLA, SKIPPED_UNMODIFIED_CACHE_HIT

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    @Column(name = "evaluated_by", length = 128)
    private String evaluatedBy;

    @Column(name = "execution_duration_ms", nullable = false)
    private Long executionDurationMs = 0L;

    @Column(name = "proof_details", columnDefinition = "TEXT")
    private String proofDetails;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true;

    public ValidationProvenance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getAphClause() { return aphClause; }
    public void setAphClause(String aphClause) { this.aphClause = aphClause; }

    public String getRuleCategory() { return ruleCategory; }
    public void setRuleCategory(String ruleCategory) { this.ruleCategory = ruleCategory; }

    public String getFieldReference() { return fieldReference; }
    public void setFieldReference(String fieldReference) { this.fieldReference = fieldReference; }

    public String getInputPayloadHash() { return inputPayloadHash; }
    public void setInputPayloadHash(String inputPayloadHash) { this.inputPayloadHash = inputPayloadHash; }

    public String getValidationOutcome() { return validationOutcome; }
    public void setValidationOutcome(String validationOutcome) { this.validationOutcome = validationOutcome; }

    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }

    public String getReEvaluationReason() { return reEvaluationReason; }
    public void setReEvaluationReason(String reEvaluationReason) { this.reEvaluationReason = reEvaluationReason; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getEvaluatedBy() { return evaluatedBy; }
    public void setEvaluatedBy(String evaluatedBy) { this.evaluatedBy = evaluatedBy; }

    public Long getExecutionDurationMs() { return executionDurationMs; }
    public void setExecutionDurationMs(Long executionDurationMs) { this.executionDurationMs = executionDurationMs; }

    public String getProofDetails() { return proofDetails; }
    public void setProofDetails(String proofDetails) { this.proofDetails = proofDetails; }

    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
}
