package in.gov.aicte.platform.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.auth.SecurityUtils;
import in.gov.aicte.platform.auth.dto.DevUserDto;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import in.gov.aicte.platform.compliance.dto.ComplianceMetricsResponse;
import in.gov.aicte.platform.compliance.dto.ComplianceResponse;
import in.gov.aicte.platform.compliance.dto.SubmitComplianceEvidenceRequest;
import in.gov.aicte.platform.compliance.dto.VerifyComplianceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);

    private final ComplianceObligationRepository obligationRepository;
    private final AuditEventRepository auditEventRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public ComplianceService(
            ComplianceObligationRepository obligationRepository,
            AuditEventRepository auditEventRepository,
            SecurityUtils securityUtils,
            ObjectMapper objectMapper
    ) {
        this.obligationRepository = obligationRepository;
        this.auditEventRepository = auditEventRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ComplianceResponse> listObligations(Long institutionId, String status, String urgency, String category, String q) {
        List<ComplianceObligation> all = obligationRepository.findAll();

        // Enforce institutional scoping if caller is an Institution Representative
        final String assignedInstId = securityUtils.hasRole(SecurityUtils.ROLE_INSTITUTION_REPRESENTATIVE)
                ? securityUtils.getCurrentUser().map(DevUserDto::institutionId).orElse(null)
                : null;

        // Enforce regional scoping if caller is a Regional Officer
        final String assignedRegion = securityUtils.hasRole(SecurityUtils.ROLE_REGIONAL_OFFICER)
                ? securityUtils.getCurrentUser().map(DevUserDto::region).orElse(null)
                : null;

        LocalDate today = LocalDate.now();

        return all.stream()
                .filter(obl -> {
                    // Update dynamic urgency
                    if (obl.getDueDate() != null && !"Verified compliant".equalsIgnoreCase(obl.getStatus())) {
                        if (obl.getDueDate().isBefore(today)) {
                            obl.setUrgency("Overdue");
                        } else if (obl.getDueDate().isBefore(today.plusDays(15))) {
                            obl.setUrgency("Due soon");
                        } else {
                            obl.setUrgency("Normal");
                        }
                    }

                    if (assignedInstId != null && !assignedInstId.isBlank()) {
                        String instIdent = obl.getInstitution() != null ? obl.getInstitution().getInstitutionalIdentifier() : null;
                        if (instIdent == null || !assignedInstId.equalsIgnoreCase(instIdent)) {
                            return false;
                        }
                    }

                    if (assignedRegion != null && !assignedRegion.isBlank()
                            && !"All India".equalsIgnoreCase(assignedRegion)
                            && !"Multi-Region".equalsIgnoreCase(assignedRegion)
                            && !"Platform-wide".equalsIgnoreCase(assignedRegion)) {
                        String reg = obl.getInstitution() != null ? obl.getInstitution().getRegion() : null;
                        if (reg == null || !assignedRegion.equalsIgnoreCase(reg)) {
                            return false;
                        }
                    }

                    if (institutionId != null) {
                        if (obl.getInstitution() == null || !institutionId.equals(obl.getInstitution().getId())) {
                            return false;
                        }
                    }

                    if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status.trim())) {
                        if (!status.trim().equalsIgnoreCase(obl.getStatus())) {
                            return false;
                        }
                    }

                    if (urgency != null && !urgency.isBlank() && !"All".equalsIgnoreCase(urgency.trim())) {
                        if (!urgency.trim().equalsIgnoreCase(obl.getUrgency())) {
                            return false;
                        }
                    }

                    if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category.trim())) {
                        if (!category.trim().equalsIgnoreCase(obl.getCategory())) {
                            return false;
                        }
                    }

                    if (q != null && !q.isBlank()) {
                        String term = q.trim().toLowerCase();
                        String ref = obl.getObligationReference() != null ? obl.getObligationReference().toLowerCase() : "";
                        String req = obl.getRequirement() != null ? obl.getRequirement().toLowerCase() : "";
                        String instName = obl.getInstitution() != null ? obl.getInstitution().getName().toLowerCase() : "";
                        return ref.contains(term) || req.contains(term) || instName.contains(term);
                    }

                    return true;
                })
                .map(ComplianceResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplianceResponse getObligationByIdOrReference(String idOrReference) {
        ComplianceObligation obl = findObligation(idOrReference);
        securityUtils.assertInstitutionAccess(obl.getInstitution());
        if (obl.getInstitution() != null) {
            securityUtils.assertRegionAccess(obl.getInstitution().getRegion());
        }
        return ComplianceResponse.from(obl);
    }

    @Transactional
    public ComplianceResponse submitEvidence(String idOrReference, SubmitComplianceEvidenceRequest request, String actorId) {
        ComplianceObligation obl = findObligation(idOrReference);
        securityUtils.assertInstitutionAccess(obl.getInstitution());

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        String actorDisplay = (request.actorName() != null && !request.actorName().isBlank()) ? request.actorName() : effectiveActor;

        obl.setStatus("Evidence submitted");
        obl.setEvidenceFile(request.evidenceFile().trim());
        if (request.evidenceHash() != null) {
            obl.setEvidenceHash(request.evidenceHash().trim());
        }
        obl.setEvidenceNotes(request.notes().trim());
        obl.setSubmittedDate(LocalDate.now());
        obl.setSubmittedBy(actorDisplay);

        ComplianceObligation saved = obligationRepository.save(obl);

        saveAuditEvent("COMPLIANCE_EVIDENCE_SUBMITTED", effectiveActor, "COMPLIANCE_OBLIGATION", saved.getObligationReference(), Map.of(
                "obligationReference", saved.getObligationReference(),
                "evidenceFile", saved.getEvidenceFile(),
                "actorName", actorDisplay
        ));

        log.info("Compliance evidence submitted for obligation={} by actor={}", saved.getObligationReference(), actorDisplay);
        return ComplianceResponse.from(saved);
    }

    @Transactional
    public ComplianceResponse markUnderVerification(String idOrReference, String actorId) {
        ComplianceObligation obl = findObligation(idOrReference);
        if (obl.getInstitution() != null) {
            securityUtils.assertRegionAccess(obl.getInstitution().getRegion());
        }

        obl.setStatus("Verification pending");
        ComplianceObligation saved = obligationRepository.save(obl);

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        saveAuditEvent("COMPLIANCE_UNDER_VERIFICATION", effectiveActor, "COMPLIANCE_OBLIGATION", saved.getObligationReference(), Map.of(
                "obligationReference", saved.getObligationReference(),
                "status", "Verification pending"
        ));

        return ComplianceResponse.from(saved);
    }

    @Transactional
    public ComplianceResponse verifyEvidence(String idOrReference, VerifyComplianceRequest request, String actorId) {
        ComplianceObligation obl = findObligation(idOrReference);
        if (obl.getInstitution() != null) {
            securityUtils.assertRegionAccess(obl.getInstitution().getRegion());
        }

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        String actorDisplay = (request.actorName() != null && !request.actorName().isBlank()) ? request.actorName() : effectiveActor;

        String outcome = request.outcome().trim();
        obl.setStatus(outcome);
        obl.setVerificationOutcome(outcome);
        if (request.remarks() != null) {
            obl.setVerificationRemarks(request.remarks().trim());
        }
        obl.setVerifiedDate(LocalDate.now());
        obl.setVerifiedBy(actorDisplay);

        ComplianceObligation saved = obligationRepository.save(obl);

        saveAuditEvent("COMPLIANCE_EVIDENCE_VERIFIED", effectiveActor, "COMPLIANCE_OBLIGATION", saved.getObligationReference(), Map.of(
                "obligationReference", saved.getObligationReference(),
                "outcome", outcome,
                "verifier", actorDisplay
        ));

        log.info("Compliance verified for obligation={} outcome='{}' by verifier={}", saved.getObligationReference(), outcome, actorDisplay);
        return ComplianceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ComplianceMetricsResponse getMetrics() {
        List<ComplianceObligation> obligations = obligationRepository.findAll();
        LocalDate today = LocalDate.now();

        long total = obligations.size();
        long dueSoon = obligations.stream()
                .filter(o -> !"Verified compliant".equalsIgnoreCase(o.getStatus()) && o.getDueDate() != null
                        && !o.getDueDate().isBefore(today) && o.getDueDate().isBefore(today.plusDays(15)))
                .count();
        long overdue = obligations.stream()
                .filter(o -> !"Verified compliant".equalsIgnoreCase(o.getStatus()) && o.getDueDate() != null
                        && o.getDueDate().isBefore(today))
                .count();
        long submitted = obligations.stream()
                .filter(o -> "Evidence submitted".equalsIgnoreCase(o.getStatus()))
                .count();
        long underVerification = obligations.stream()
                .filter(o -> "Verification pending".equalsIgnoreCase(o.getStatus()))
                .count();
        long clarification = obligations.stream()
                .filter(o -> "Clarification requested".equalsIgnoreCase(o.getStatus()))
                .count();
        long verified = obligations.stream()
                .filter(o -> "Verified compliant".equalsIgnoreCase(o.getStatus()))
                .count();

        return new ComplianceMetricsResponse(total, dueSoon, overdue, submitted, underVerification, clarification, verified);
    }

    private ComplianceObligation findObligation(String idOrReference) {
        if (idOrReference == null || idOrReference.isBlank()) {
            throw new ResourceNotFoundException("Obligation identifier cannot be empty.");
        }

        try {
            Long numericId = Long.parseLong(idOrReference.trim());
            return obligationRepository.findById(numericId)
                    .orElseGet(() -> obligationRepository.findByObligationReference(idOrReference.trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found for ID: " + idOrReference)));
        } catch (NumberFormatException ignored) {
        }

        return obligationRepository.findByObligationReference(idOrReference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found for reference: " + idOrReference));
    }

    private void saveAuditEvent(String eventType, String actorId, String targetType, String targetId, Map<String, Object> metadata) {
        String metaStr;
        try {
            metaStr = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            metaStr = "{}";
        }
        AuditEvent event = new AuditEvent(eventType, actorId, targetType, targetId, metaStr);
        auditEventRepository.save(event);
    }
}
