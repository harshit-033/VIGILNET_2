package in.gov.aicte.platform.evc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import in.gov.aicte.platform.evc.dto.CompleteVisitRequest;
import in.gov.aicte.platform.evc.dto.EvcMetricsResponse;
import in.gov.aicte.platform.evc.dto.EvcResponse;
import in.gov.aicte.platform.evc.dto.RescheduleEvcRequest;
import in.gov.aicte.platform.evc.dto.ScheduleEvcRequest;
import in.gov.aicte.platform.evc.dto.SubmitEvcReportRequest;
import in.gov.aicte.platform.workflow.WorkflowTransition;
import in.gov.aicte.platform.workflow.WorkflowTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EvcService {

    private static final Logger log = LoggerFactory.getLogger(EvcService.class);

    private final EvcVisitRepository evcVisitRepository;
    private final ApplicationRepository applicationRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    private final in.gov.aicte.platform.auth.SecurityUtils securityUtils;

    public EvcService(
            EvcVisitRepository evcVisitRepository,
            ApplicationRepository applicationRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper,
            in.gov.aicte.platform.auth.SecurityUtils securityUtils
    ) {
        this.evcVisitRepository = evcVisitRepository;
        this.applicationRepository = applicationRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<EvcResponse> listVisits(String status, String region, String q) {
        List<EvcVisit> all = evcVisitRepository.findAll();

        String effectiveRegion = region;
        if (securityUtils.hasRole(in.gov.aicte.platform.auth.SecurityUtils.ROLE_REGIONAL_OFFICER)) {
            java.util.Optional<in.gov.aicte.platform.auth.dto.DevUserDto> userOpt = securityUtils.getCurrentUser();
            if (userOpt.isPresent() && userOpt.get().region() != null && (effectiveRegion == null || effectiveRegion.isBlank())) {
                String r = userOpt.get().region().trim();
                if (!"All India".equalsIgnoreCase(r) && !"Multi-Region".equalsIgnoreCase(r) && !"Platform-wide".equalsIgnoreCase(r)) {
                    effectiveRegion = r;
                }
            }
        }

        final String finalRegion = effectiveRegion;
        final String assignedInstId = securityUtils.hasRole(in.gov.aicte.platform.auth.SecurityUtils.ROLE_INSTITUTION_REPRESENTATIVE)
                ? securityUtils.getCurrentUser().map(in.gov.aicte.platform.auth.dto.DevUserDto::institutionId).orElse(null)
                : null;

        return all.stream()
                .filter(v -> {
                    if (assignedInstId != null && !assignedInstId.isBlank()) {
                        String instIdent = (v.getApplication() != null && v.getApplication().getInstitution() != null)
                                ? v.getApplication().getInstitution().getInstitutionalIdentifier()
                                : null;
                        if (instIdent == null || !assignedInstId.equalsIgnoreCase(instIdent)) {
                            return false;
                        }
                    }
                    if (status != null && !status.isBlank()) {
                        if (!status.equalsIgnoreCase(v.getStatus())) {
                            return false;
                        }
                    }
                    if (finalRegion != null && !finalRegion.isBlank()) {
                        String reg = (v.getApplication() != null && v.getApplication().getInstitution() != null)
                                ? v.getApplication().getInstitution().getRegion()
                                : null;
                        if (reg == null || !reg.equalsIgnoreCase(finalRegion.trim())) {
                            return false;
                        }
                    }
                    if (q != null && !q.isBlank()) {
                        String term = q.trim().toLowerCase();
                        String ref = v.getEvcReference() != null ? v.getEvcReference().toLowerCase() : "";
                        String appRef = (v.getApplication() != null && v.getApplication().getApplicationReference() != null)
                                ? v.getApplication().getApplicationReference().toLowerCase()
                                : "";
                        String instName = (v.getApplication() != null && v.getApplication().getInstitution() != null)
                                ? v.getApplication().getInstitution().getName().toLowerCase()
                                : "";
                        String coord = v.getCoordinatorName() != null ? v.getCoordinatorName().toLowerCase() : "";

                        return ref.contains(term) || appRef.contains(term) || instName.contains(term) || coord.contains(term);
                    }
                    return true;
                })
                .map(EvcResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EvcResponse getVisitByIdOrReference(String idOrReference) {
        EvcVisit visit = findVisit(idOrReference);
        assertVisitAccess(visit);
        return EvcResponse.from(visit);
    }

    @Transactional(readOnly = true)
    public EvcMetricsResponse getMetrics() {
        List<EvcVisit> visits = evcVisitRepository.findAll();
        LocalDate today = LocalDate.now();

        long totalCases = visits.size();
        long pendingScheduling = visits.stream()
                .filter(v -> "Awaiting scheduling".equalsIgnoreCase(v.getStatus()))
                .count();
        long upcomingVisits = visits.stream()
                .filter(v -> "Scheduled".equalsIgnoreCase(v.getStatus()))
                .count();
        long completedVisits = visits.stream()
                .filter(v -> "Completed".equalsIgnoreCase(v.getStatus()) || "Complete for stage".equalsIgnoreCase(v.getStatus()))
                .count();
        long reportsPending = visits.stream()
                .filter(v -> "Report pending".equalsIgnoreCase(v.getStatus()) ||
                        ("Completed".equalsIgnoreCase(v.getStatus()) && !"Complete for stage".equalsIgnoreCase(v.getStatus())))
                .count();
        long overdueTasks = visits.stream()
                .filter(v -> "Overdue".equalsIgnoreCase(v.getUrgency()) ||
                        (v.getDeadline() != null && v.getDeadline().isBefore(today) && !"Complete for stage".equalsIgnoreCase(v.getStatus())))
                .count();

        return new EvcMetricsResponse(
                totalCases,
                pendingScheduling,
                upcomingVisits,
                completedVisits,
                reportsPending,
                overdueTasks
        );
    }

    @Transactional
    public EvcResponse scheduleVisit(String idOrReference, ScheduleEvcRequest request, String actorId) {
        EvcVisit visit = findVisit(idOrReference);
        assertVisitAccess(visit);

        if ("Completed".equalsIgnoreCase(visit.getStatus()) || "Complete for stage".equalsIgnoreCase(visit.getStatus())) {
            throw new IllegalArgumentException(String.format("Cannot schedule an inspection that is already '%s'. Revert visit status first if rescheduling is needed.", visit.getStatus()));
        }

        if (request.visitDate() == null) {
            throw new IllegalArgumentException("A confirmed inspection visit date is required.");
        }

        visit.setVisitDate(request.visitDate());
        if (request.coordinator() != null && !request.coordinator().isBlank()) {
            visit.setCoordinatorName(request.coordinator().trim());
        }
        if (request.inspectionType() != null && !request.inspectionType().isBlank()) {
            visit.setInspectionType(request.inspectionType().trim());
        }

        if (request.experts() != null && !request.experts().isEmpty()) {
            try {
                visit.setExpertsJson(objectMapper.writeValueAsString(request.experts()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize experts JSON: {}", e.getMessage());
            }
        }

        visit.setStatus("Scheduled");
        visit.setUrgency("In progress");

        EvcVisit saved = evcVisitRepository.save(visit);

        // Record audit
        saveAuditEvent("EVC_VISIT_SCHEDULED", actorId, "EVC_VISIT", saved.getEvcReference(), Map.of(
                "visitDate", saved.getVisitDate().toString(),
                "coordinator", saved.getCoordinatorName(),
                "inspectionType", saved.getInspectionType(),
                "actorName", request.actorName() != null ? request.actorName() : actorId
        ));

        return EvcResponse.from(saved);
    }

    @Transactional
    public EvcResponse completeVisit(String idOrReference, CompleteVisitRequest request, String actorId) {
        EvcVisit visit = findVisit(idOrReference);
        assertVisitAccess(visit);

        if (!"Scheduled".equalsIgnoreCase(visit.getStatus())) {
            throw new IllegalArgumentException(String.format("Only visits in 'Scheduled' state can be marked as completed. Current status is '%s'.", visit.getStatus()));
        }

        visit.setStatus("Completed");
        visit.setReportStatus("Report pending submission");
        if (request.completionNotes() != null && !request.completionNotes().isBlank()) {
            visit.setCompletionNotes(request.completionNotes().trim());
        }

        EvcVisit saved = evcVisitRepository.save(visit);

        // Record audit
        saveAuditEvent("EVC_VISIT_COMPLETED", actorId, "EVC_VISIT", saved.getEvcReference(), Map.of(
                "status", "Completed",
                "completionNotes", saved.getCompletionNotes() != null ? saved.getCompletionNotes() : "",
                "actorName", request.actorName() != null ? request.actorName() : actorId
        ));

        return EvcResponse.from(saved);
    }

    @Transactional
    public EvcResponse submitReport(String idOrReference, SubmitEvcReportRequest request, String actorId) {
        EvcVisit visit = findVisit(idOrReference);
        assertVisitAccess(visit);

        if (!"Completed".equalsIgnoreCase(visit.getStatus()) && !"Report pending".equalsIgnoreCase(visit.getStatus())) {
            throw new IllegalArgumentException(String.format("Inspection report can only be submitted after visit completion. Current status is '%s'.", visit.getStatus()));
        }

        if (request.reportOutcome() == null || request.reportOutcome().isBlank()) {
            throw new IllegalArgumentException("Inspection report recommendation/outcome is mandatory.");
        }

        visit.setStatus("Complete for stage");
        visit.setReportStatus("Report Submitted & Verified");
        visit.setReportOutcome(request.reportOutcome().trim());
        if (request.reportNotes() != null && !request.reportNotes().isBlank()) {
            visit.setReportNotes(request.reportNotes().trim());
        }

        EvcVisit saved = evcVisitRepository.save(visit);

        // Advance linked Application to 'Committee review'
        Application application = saved.getApplication();
        if (application != null) {
            String fromStage = application.getCurrentStage() != null ? application.getCurrentStage() : "EVC scheduling";
            String fromStatus = application.getStatus();
            String toStage = "Committee review";
            String toStatus = "In progress";

            application.setCurrentStage(toStage);
            application.setStatus(toStatus);
            application.setOwnerName("Standing Scrutiny / Hearing Committee");
            application.setOwnerId("USR-SAC-01");
            applicationRepository.save(application);

            // Record workflow transition
            String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
            String actorDisplay = (request.actorName() != null && !request.actorName().isBlank()) ? request.actorName() : effectiveActor;
            String actorRole = (request.actorRole() != null && !request.actorRole().isBlank()) ? request.actorRole() : "EVC Coordinator";

            WorkflowTransition transition = new WorkflowTransition(
                    application,
                    "SUBMIT_EVC_REPORT",
                    fromStage,
                    toStage,
                    fromStatus,
                    toStatus,
                    effectiveActor,
                    actorDisplay,
                    actorRole,
                    "EVC Inspection report submitted with outcome: " + saved.getReportOutcome(),
                    saved.getReportNotes(),
                    null
            );
            workflowTransitionRepository.save(transition);

            // Record workflow audit event
            saveAuditEvent("WORKFLOW_ACTION_EXECUTED", effectiveActor, "APPLICATION", application.getApplicationReference(), Map.of(
                    "action", "SUBMIT_EVC_REPORT",
                    "fromStage", fromStage,
                    "toStage", toStage,
                    "reportOutcome", saved.getReportOutcome(),
                    "actorName", actorDisplay
            ));
        }

        // Record EVC audit event
        saveAuditEvent("EVC_REPORT_SUBMITTED", actorId, "EVC_VISIT", saved.getEvcReference(), Map.of(
                "reportOutcome", saved.getReportOutcome(),
                "actorName", request.actorName() != null ? request.actorName() : actorId
        ));

        return EvcResponse.from(saved);
    }

    @Transactional
    public EvcResponse rescheduleVisit(String idOrReference, RescheduleEvcRequest request, String actorId) {
        EvcVisit visit = findVisit(idOrReference);
        assertVisitAccess(visit);

        visit.setStatus("Scheduled");
        visit.setReportStatus("Not Applicable");
        if (request.newVisitDate() != null) {
            visit.setVisitDate(request.newVisitDate());
        }

        EvcVisit saved = evcVisitRepository.save(visit);

        saveAuditEvent("EVC_VISIT_RESCHEDULED", actorId, "EVC_VISIT", saved.getEvcReference(), Map.of(
                "status", "Scheduled",
                "newVisitDate", saved.getVisitDate() != null ? saved.getVisitDate().toString() : "Unchanged",
                "actorName", request.actorName() != null ? request.actorName() : actorId
        ));

        return EvcResponse.from(saved);
    }

    private void assertVisitAccess(EvcVisit visit) {
        if (visit != null && visit.getApplication() != null && visit.getApplication().getInstitution() != null) {
            securityUtils.assertInstitutionAccess(visit.getApplication().getInstitution());
            securityUtils.assertRegionAccess(visit.getApplication().getInstitution().getRegion());
        }
    }

    @Transactional
    public EvcVisit ensureEvcVisitForApplication(Application application) {
        if (application == null || application.getId() == null) {
            return null;
        }

        return evcVisitRepository.findByApplicationId(application.getId())
                .orElseGet(() -> {
                    String ref = "EVC-" + application.getId();
                    EvcVisit visit = new EvcVisit(
                            application,
                            ref,
                            application.getOwnerName() != null ? application.getOwnerName() : "C. Verma (EVC Operations)",
                            application.getOwnerId() != null ? application.getOwnerId() : "USR-EVC-01"
                    );
                    if (application.getDueDate() != null) {
                        visit.setDeadline(application.getDueDate());
                    } else {
                        visit.setDeadline(LocalDate.now().plusDays(14));
                    }
                    EvcVisit created = evcVisitRepository.save(visit);
                    log.info("Created new EVC visit record {} for application {}", ref, application.getApplicationReference());
                    return created;
                });
    }

    private EvcVisit findVisit(String idOrReference) {
        if (idOrReference == null || idOrReference.isBlank()) {
            throw new ResourceNotFoundException("EVC visit identifier cannot be empty.");
        }

        // Try numeric ID
        try {
            Long numericId = Long.parseLong(idOrReference.trim());
            return evcVisitRepository.findById(numericId)
                    .orElseGet(() -> evcVisitRepository.findByApplicationId(numericId)
                            .orElseThrow(() -> new ResourceNotFoundException("EVC visit not found for ID: " + idOrReference)));
        } catch (NumberFormatException ignored) {
        }

        // Try EVC reference (e.g. EVC-1)
        return evcVisitRepository.findByEvcReference(idOrReference.trim())
                .or(() -> evcVisitRepository.findByApplicationApplicationReference(idOrReference.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("EVC visit not found for reference: " + idOrReference));
    }

    private void saveAuditEvent(String eventType, String actorId, String targetType, String targetId, Map<String, Object> metadata) {
        String metaStr;
        try {
            metaStr = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            metaStr = "{}";
        }
        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        AuditEvent event = new AuditEvent(eventType, effectiveActor, targetType, targetId, metaStr);
        auditEventRepository.save(event);
    }
}
