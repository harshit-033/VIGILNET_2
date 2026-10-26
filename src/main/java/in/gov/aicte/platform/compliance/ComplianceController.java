package in.gov.aicte.platform.compliance;

import in.gov.aicte.platform.compliance.dto.ComplianceMetricsResponse;
import in.gov.aicte.platform.compliance.dto.ComplianceResponse;
import in.gov.aicte.platform.compliance.dto.SubmitComplianceEvidenceRequest;
import in.gov.aicte.platform.compliance.dto.VerifyComplianceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance")
@Tag(name = "Compliance Management", description = "Continuous statutory compliance obligations and evidence verification endpoints")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List compliance obligations", description = "Retrieve list of statutory obligations with optional filtering")
    public ResponseEntity<List<ComplianceResponse>> listObligations(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(complianceService.listObligations(institutionId, status, urgency, category, q));
    }

    @GetMapping("/metrics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Compliance metrics", description = "Get overview KPIs of statutory obligations across stages")
    public ResponseEntity<ComplianceMetricsResponse> getMetrics() {
        return ResponseEntity.ok(complianceService.getMetrics());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get obligation by ID or Reference", description = "Retrieve single compliance obligation details")
    public ResponseEntity<ComplianceResponse> getObligationById(@PathVariable String id) {
        return ResponseEntity.ok(complianceService.getObligationByIdOrReference(id));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INSTITUTION_REPRESENTATIVE', 'ADMINISTRATOR', 'USER')")
    @Operation(summary = "Submit compliance evidence", description = "Upload affidavit or evidence proof for a condition")
    public ResponseEntity<ComplianceResponse> submitEvidence(
            @PathVariable String id,
            @Valid @RequestBody SubmitComplianceEvidenceRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(complianceService.submitEvidence(id, request, actorId));
    }

    @PostMapping("/{id}/under-verification")
    @PreAuthorize("hasAnyRole('COMPLIANCE_REVIEWER', 'REGIONAL_OFFICER', 'ADMINISTRATOR', 'USER')")
    @Operation(summary = "Mark compliance under verification", description = "Initiate documentary review by bureau officer")
    public ResponseEntity<ComplianceResponse> markUnderVerification(
            @PathVariable String id,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(complianceService.markUnderVerification(id, actorId));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('COMPLIANCE_REVIEWER', 'REGIONAL_OFFICER', 'SENIOR_MANAGEMENT', 'ADMINISTRATOR', 'USER')")
    @Operation(summary = "Verify compliance evidence", description = "Record official regulatory decision on submitted proof")
    public ResponseEntity<ComplianceResponse> verifyEvidence(
            @PathVariable String id,
            @Valid @RequestBody VerifyComplianceRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(complianceService.verifyEvidence(id, request, actorId));
    }
}
