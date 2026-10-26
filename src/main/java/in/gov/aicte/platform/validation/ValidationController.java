package in.gov.aicte.platform.validation;

import in.gov.aicte.platform.validation.dto.EvaluateApplicationRequest;
import in.gov.aicte.platform.validation.dto.ValidationProvenanceResponse;
import in.gov.aicte.platform.validation.dto.ValidationReportResponse;
import in.gov.aicte.platform.validation.rules.AphRuleDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

    private final IncrementalValidationService validationService;

    public ValidationController(IncrementalValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/{applicationId}/evaluate")
    @PreAuthorize("hasAnyRole('ROLE_INSTITUTION_REPRESENTATIVE', 'ROLE_SCRUTINY_OFFICER', 'ROLE_REGIONAL_OFFICER', 'ROLE_COMPLIANCE_REVIEWER', 'ROLE_SENIOR_MANAGEMENT', 'ROLE_ADMINISTRATOR')")
    public ResponseEntity<ValidationReportResponse> evaluateApplication(
            @PathVariable("applicationId") String applicationId,
            @RequestBody(required = false) EvaluateApplicationRequest request
    ) {
        EvaluateApplicationRequest effectiveRequest = (request != null)
                ? request
                : new EvaluateApplicationRequest(Map.of(), false, null, null);

        ValidationReportResponse report = validationService.evaluateApplication(applicationId, effectiveRequest);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/{applicationId}/provenance")
    @PreAuthorize("hasAnyRole('ROLE_INSTITUTION_REPRESENTATIVE', 'ROLE_SCRUTINY_OFFICER', 'ROLE_REGIONAL_OFFICER', 'ROLE_COMPLIANCE_REVIEWER', 'ROLE_SENIOR_MANAGEMENT', 'ROLE_ADMINISTRATOR', 'ROLE_AUDITOR')")
    public ResponseEntity<List<ValidationProvenanceResponse>> getProvenanceHistory(
            @PathVariable("applicationId") String applicationId
    ) {
        List<ValidationProvenanceResponse> history = validationService.getProvenanceHistory(applicationId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/rules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AphRuleDefinition>> getAllRules() {
        return ResponseEntity.ok(validationService.getAllRules());
    }
}
