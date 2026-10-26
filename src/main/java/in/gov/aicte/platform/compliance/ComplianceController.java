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
    @PreAuthorize("hasAnyRole('INSTITUTION_REPRESENTATIVE', 'ADMINISTRATOR')")
    @Operation(summary = "Submit compliance evidence (Metadata)", description = "Submit compliance evidence metadata for a condition")
    public ResponseEntity<ComplianceResponse> submitEvidence(
            @PathVariable String id,
            @Valid @RequestBody SubmitComplianceEvidenceRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(complianceService.submitEvidence(id, request, actorId));
    }

    @PostMapping(value = "/{id}/evidence", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INSTITUTION_REPRESENTATIVE', 'ADMINISTRATOR')")
    @Operation(summary = "Upload compliance evidence file", description = "Upload physical evidence proof file (PDF, image, document) for a statutory condition")
    public ResponseEntity<ComplianceResponse> uploadEvidence(
            @PathVariable String id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "actorName", required = false) String actorName,
            Principal principal
    ) throws java.io.IOException {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(complianceService.uploadEvidence(id, file, notes, actorName, actorId));
    }

    @GetMapping("/{id}/evidence/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download compliance evidence file", description = "Stream uploaded statutory evidence proof from durable storage")
    public ResponseEntity<org.springframework.core.io.Resource> downloadEvidence(@PathVariable String id) {
        ComplianceResponse obl = complianceService.getObligationByIdOrReference(id);
        org.springframework.core.io.Resource resource = complianceService.loadEvidenceResource(id);

        org.springframework.http.MediaType mediaType;
        try {
            mediaType = obl.mimeType() != null ? org.springframework.http.MediaType.parseMediaType(obl.mimeType()) : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        }

        String filename = obl.evidenceFile() != null ? obl.evidenceFile() : "evidence_" + id + ".pdf";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/under-verification")
    @PreAuthorize("hasAnyRole('COMPLIANCE_REVIEWER', 'REGIONAL_OFFICER', 'ADMINISTRATOR')")
    @Operation(summary = "Mark compliance under verification", description = "Initiate documentary review by bureau officer")
    public ResponseEntity<ComplianceResponse> markUnderVerification(
            @PathVariable String id,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(complianceService.markUnderVerification(id, actorId));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('COMPLIANCE_REVIEWER', 'REGIONAL_OFFICER', 'SENIOR_MANAGEMENT', 'ADMINISTRATOR')")
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
