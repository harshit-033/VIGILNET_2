package in.gov.aicte.platform.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.application.checklist.ApplicationChecklist;
import in.gov.aicte.platform.application.checklist.ApplicationChecklistRepository;
import in.gov.aicte.platform.application.checklist.dto.ChecklistItemResponse;
import in.gov.aicte.platform.application.deficiency.ApplicationDeficiency;
import in.gov.aicte.platform.application.deficiency.ApplicationDeficiencyRepository;
import in.gov.aicte.platform.application.deficiency.dto.DeficiencyResponse;
import in.gov.aicte.platform.application.dto.ApplicationResponse;
import in.gov.aicte.platform.application.submission.ApplicationSubmission;
import in.gov.aicte.platform.application.submission.ApplicationSubmissionRepository;
import in.gov.aicte.platform.application.submission.dto.SubmissionResponse;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import in.gov.aicte.platform.auth.SecurityUtils;
import in.gov.aicte.platform.evc.EvcService;
import in.gov.aicte.platform.workflow.dto.WorkflowActionRequest;
import in.gov.aicte.platform.workflow.dto.WorkflowTimelineItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final ApplicationRepository applicationRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final AuditEventRepository auditEventRepository;
    private final ApplicationDeficiencyRepository deficiencyRepository;
    private final ApplicationSubmissionRepository submissionRepository;
    private final ApplicationChecklistRepository checklistRepository;
    private final ObjectMapper objectMapper;
    private final EvcService evcService;
    private final SecurityUtils securityUtils;

    public WorkflowService(
            ApplicationRepository applicationRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            AuditEventRepository auditEventRepository,
            ApplicationDeficiencyRepository deficiencyRepository,
            ApplicationSubmissionRepository submissionRepository,
            ApplicationChecklistRepository checklistRepository,
            ObjectMapper objectMapper,
            EvcService evcService,
            SecurityUtils securityUtils
    ) {
        this.applicationRepository = applicationRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.auditEventRepository = auditEventRepository;
        this.deficiencyRepository = deficiencyRepository;
        this.submissionRepository = submissionRepository;
        this.checklistRepository = checklistRepository;
        this.objectMapper = objectMapper;
        this.evcService = evcService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public ApplicationResponse executeAction(String idOrReference, WorkflowActionRequest request, String actorId) {
        Application application = findApplication(idOrReference);

        // Security guards: Institutional tenant scoping, regional isolation, and action RBAC
        securityUtils.assertInstitutionAccess(application.getInstitution());
        securityUtils.assertRegionAccess(application.getInstitution().getRegion());
        securityUtils.assertWorkflowActionAllowed(request.actionType());

        String action = request.actionType() != null ? request.actionType().trim().toUpperCase() : "";
        String fromStage = application.getCurrentStage() != null ? application.getCurrentStage() : "Scrutiny";
        String fromStatus = application.getStatus();
        String toStage = fromStage;
        String toStatus = fromStatus;
        String deficienciesJson = null;

        String rawEffectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        final String effectiveActor = (rawEffectiveActor.length() > 64) ? rawEffectiveActor.substring(0, 64) : rawEffectiveActor;

        String rawActorDisplay = (request.actorName() != null && !request.actorName().isBlank()) ? request.actorName() : effectiveActor;
        final String actorDisplay = (rawActorDisplay.length() > 128) ? rawActorDisplay.substring(0, 128) : rawActorDisplay;

        switch (action) {
            case "SUBMIT_APPLICATION" -> {
                if (!"DRAFT".equalsIgnoreCase(fromStatus)) {
                    throw new IllegalArgumentException(String.format("Cannot submit application with status '%s'. Application must be in 'DRAFT'.", fromStatus));
                }
                toStage = "Scrutiny";
                toStatus = "In progress";
                application.setCurrentStage(toStage);
                application.setStatus(toStatus);
                application.setOwnerName("B. Singh (Scrutiny Officer)");
                application.setOwnerId("USR-SO-01");

                // Record initial submission version if none exists
                if (submissionRepository.countByApplicationId(application.getId()) == 0) {
                    ApplicationSubmission sub = new ApplicationSubmission(
                            application,
                            "SUB-" + application.getId() + "-v1",
                            "INITIAL_DOSSIER",
                            1,
                            effectiveActor,
                            actorDisplay,
                            request.reason() != null ? request.reason() : "Application dossier submitted for official scrutiny.",
                            "SUBMITTED"
                    );
                    submissionRepository.save(sub);
                }
            }

            case "RETURN_FOR_CORRECTION" -> {
                if (!"Scrutiny".equalsIgnoreCase(fromStage) && !"Re-scrutiny".equalsIgnoreCase(fromStage)) {
                    throw new IllegalArgumentException(String.format("Cannot return application from stage '%s'. Must be in Scrutiny or Re-scrutiny.", fromStage));
                }
                if (request.reason() == null || request.reason().trim().isBlank()) {
                    throw new IllegalArgumentException("A mandatory reason is required to return an application for correction.");
                }

                toStage = "Re-scrutiny";
                toStatus = "Returned";

                List<String> rawDefs = (request.newDeficiencies() != null && !request.newDeficiencies().isEmpty())
                        ? request.newDeficiencies()
                        : List.of(request.reason().trim());

                // Persist itemized deficiency entities with requirement references
                int seq = 1;
                List<String> deficiencySummaries = new ArrayList<>();
                for (String defText : rawDefs) {
                    if (defText != null && !defText.trim().isBlank()) {
                        String trimmedDef = defText.trim();
                        String code = "DEF-" + application.getId() + "-" + seq++;
                        String reqRef = request.requirementReference() != null && !request.requirementReference().isBlank()
                                ? request.requirementReference().trim()
                                : "APH Statutory Norms";

                        ApplicationDeficiency deficiency = new ApplicationDeficiency(
                                application,
                                code,
                                reqRef,
                                trimmedDef,
                                "OPEN",
                                actorDisplay
                        );
                        deficiencyRepository.save(deficiency);
                        deficiencySummaries.add(code + ": " + trimmedDef);
                    }
                }

                try {
                    deficienciesJson = objectMapper.writeValueAsString(deficiencySummaries);
                } catch (JsonProcessingException e) {
                    deficienciesJson = "[\"" + request.reason().replace("\"", "\\\"") + "\"]";
                }

                application.setCurrentStage(toStage);
                application.setStatus(toStatus);
                application.setOwnerName("Institution Representative");
                application.setOwnerId("USR-INST-01");
                application.setDeficienciesJson(deficienciesJson);
                if (request.observations() != null && !request.observations().isBlank()) {
                    application.setObservations(request.observations().trim());
                }
            }

            case "SUBMIT_CORRECTIONS" -> {
                if (!"Returned".equalsIgnoreCase(fromStatus)) {
                    throw new IllegalArgumentException(String.format("Cannot submit corrections for application with status '%s'. Status must be 'Returned'.", fromStatus));
                }
                if (request.reason() == null || request.reason().trim().isBlank()) {
                    throw new IllegalArgumentException("An explanatory note or reason is required when submitting rectifications.");
                }

                toStage = "Re-scrutiny";
                toStatus = "In progress";

                application.setCurrentStage(toStage);
                application.setStatus(toStatus);
                application.setOwnerName("B. Singh (Scrutiny Officer)");
                application.setOwnerId("USR-SO-01");

                // Create a new submission package version
                int nextVersion = (int) submissionRepository.countByApplicationId(application.getId()) + 1;
                ApplicationSubmission submission = new ApplicationSubmission(
                        application,
                        "SUB-" + application.getId() + "-v" + nextVersion,
                        "RECTIFICATION_PACKAGE",
                        nextVersion,
                        effectiveActor,
                        actorDisplay,
                        request.reason().trim(),
                        "SUBMITTED"
                );
                submissionRepository.save(submission);

                // Update existing OPEN or RE_ISSUED deficiencies to RECTIFIED_PENDING_REVIEW with response
                List<ApplicationDeficiency> existingDefs = deficiencyRepository.findByApplicationIdOrderByCreatedAtAsc(application.getId());
                for (ApplicationDeficiency def : existingDefs) {
                    if ("OPEN".equalsIgnoreCase(def.getStatus()) || "RE_ISSUED".equalsIgnoreCase(def.getStatus())) {
                        def.setStatus("RECTIFIED_PENDING_REVIEW");
                        def.setInstitutionResponse(request.reason().trim());
                        deficiencyRepository.save(def);
                    }
                }
            }

            case "RESOLVE_DEFICIENCY" -> {
                if (!"Scrutiny".equalsIgnoreCase(fromStage) && !"Re-scrutiny".equalsIgnoreCase(fromStage)) {
                    throw new IllegalArgumentException(String.format("Deficiencies can only be resolved during Scrutiny or Re-scrutiny (current stage: '%s').", fromStage));
                }
                if (request.deficiencyId() == null) {
                    throw new IllegalArgumentException("A valid deficiency ID is required to resolve a deficiency.");
                }

                ApplicationDeficiency def = deficiencyRepository.findById(request.deficiencyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Deficiency not found with id: " + request.deficiencyId()));

                if (!def.getApplication().getId().equals(application.getId())) {
                    throw new IllegalArgumentException("Deficiency does not belong to the target application.");
                }

                String resolution = request.resolutionStatus() != null && !request.resolutionStatus().isBlank()
                        ? request.resolutionStatus().trim().toUpperCase()
                        : "RESOLVED";

                def.setStatus(resolution);
                def.setResolutionRemarks(request.resolutionRemarks() != null ? request.resolutionRemarks().trim() : "Resolved during re-scrutiny");
                def.setResolvedBy(actorDisplay);
                deficiencyRepository.save(def);
            }

            case "UPDATE_CHECKLIST" -> {
                if (request.checklistItemCode() == null || request.checklistItemCode().isBlank()) {
                    throw new IllegalArgumentException("Checklist item code is required.");
                }
                String code = request.checklistItemCode().trim();
                boolean isVerified = Boolean.TRUE.equals(request.checklistItemVerified());

                ApplicationChecklist item = checklistRepository.findByApplicationIdAndItemCode(application.getId(), code)
                        .orElseGet(() -> new ApplicationChecklist(
                                application,
                                code,
                                request.reason() != null ? request.reason() : code,
                                "APH Norms",
                                isVerified,
                                actorDisplay
                        ));

                item.setVerified(isVerified);
                item.setVerifiedBy(actorDisplay);
                if (request.resolutionRemarks() != null) {
                    item.setRemarks(request.resolutionRemarks().trim());
                }
                checklistRepository.save(item);
            }

            case "COMPLETE_SCRUTINY" -> {
                if (!"Scrutiny".equalsIgnoreCase(fromStage) && !"Re-scrutiny".equalsIgnoreCase(fromStage)) {
                    throw new IllegalArgumentException(String.format("Cannot clear scrutiny from stage '%s'. Must be in Scrutiny or Re-scrutiny.", fromStage));
                }
                if ("Returned".equalsIgnoreCase(fromStatus)) {
                    throw new IllegalArgumentException("Cannot complete scrutiny while application status is 'Returned'. Rectifications must be submitted and re-scrutinized first.");
                }

                // Check for unresolved deficiencies
                long unresolvedCount = deficiencyRepository.countByApplicationIdAndStatusIn(
                        application.getId(),
                        List.of("OPEN", "RECTIFIED_PENDING_REVIEW", "RE_ISSUED")
                );
                if (unresolvedCount > 0) {
                    throw new IllegalArgumentException(String.format(
                            "Cannot complete scrutiny: %d deficiency item(s) remain unresolved. All deficiencies must be formally resolved by the scrutiny officer before clearance.",
                            unresolvedCount
                    ));
                }

                // Only allow destinations represented by the current workflow model.
                // Arbitrary client-provided stage names must never enter persisted state.
                String requestedStage = (request.nextStage() != null && !request.nextStage().isBlank())
                        ? request.nextStage().trim()
                        : "EVC scheduling";
                if ("EVC scheduling".equalsIgnoreCase(requestedStage)) {
                    toStage = "EVC scheduling";
                } else if ("Committee review".equalsIgnoreCase(requestedStage)) {
                    toStage = "Committee review";
                } else {
                    throw new IllegalArgumentException(
                            "Unsupported next scrutiny stage: " + requestedStage
                                    + ". Allowed destinations are 'EVC scheduling' and 'Committee review'."
                    );
                }

                List<ApplicationChecklist> checklist = checklistRepository.findByApplicationIdOrderByItemCodeAsc(application.getId());
                if (checklist.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Cannot complete scrutiny: no statutory checklist items are configured for this application."
                    );
                }
                long unverifiedChecklistCount = checklist.stream()
                        .filter(item -> !Boolean.TRUE.equals(item.getVerified()))
                        .count();
                if (unverifiedChecklistCount > 0) {
                    throw new IllegalArgumentException(String.format(
                            "Cannot complete scrutiny: %d statutory checklist item(s) remain unverified. Each item must be explicitly verified by an authorized officer.",
                            unverifiedChecklistCount
                    ));
                }

                toStatus = "Due soon";
                application.setCurrentStage(toStage);
                application.setStatus(toStatus);
                if ("EVC scheduling".equalsIgnoreCase(toStage)) {
                    application.setOwnerName("C. Verma (EVC Coordinator)");
                    application.setOwnerId("USR-EVC-01");
                } else {
                    application.setOwnerName("Regional Committee Chair");
                    application.setOwnerId("USR-RC-01");
                }

                if (request.observations() != null && !request.observations().isBlank()) {
                    application.setObservations(request.observations().trim());
                }
            }

            case "UPDATE_OBSERVATIONS" -> {
                if (request.observations() != null) {
                    application.setObservations(request.observations().trim());
                }
            }

            default -> throw new IllegalArgumentException("Unsupported workflow action: " + request.actionType());
        }

        Application savedApp = applicationRepository.save(application);

        // If stage transitioned to EVC scheduling, ensure an EVC work item exists
        if ("EVC scheduling".equalsIgnoreCase(toStage)) {
            evcService.ensureEvcVisitForApplication(savedApp);
        }

        // Record workflow transition
        WorkflowTransition transition = new WorkflowTransition(
                savedApp,
                action,
                fromStage,
                toStage,
                fromStatus,
                toStatus,
                effectiveActor,
                actorDisplay,
                request.actorRole(),
                request.reason(),
                request.observations(),
                deficienciesJson
        );
        workflowTransitionRepository.save(transition);

        // Record AuditEvent atomically in same transaction
        Map<String, Object> auditMeta = new LinkedHashMap<>();
        auditMeta.put("action", action);
        auditMeta.put("applicationReference", savedApp.getApplicationReference());
        auditMeta.put("fromStage", fromStage);
        auditMeta.put("toStage", toStage);
        auditMeta.put("fromStatus", fromStatus);
        auditMeta.put("toStatus", toStatus);
        auditMeta.put("reason", request.reason());
        auditMeta.put("actorName", actorDisplay);

        String auditMetadata;
        try {
            auditMetadata = objectMapper.writeValueAsString(auditMeta);
        } catch (JsonProcessingException e) {
            auditMetadata = "{}";
        }

        AuditEvent auditEvent = new AuditEvent(
                "WORKFLOW_ACTION_" + action,
                effectiveActor,
                "APPLICATION",
                savedApp.getId().toString(),
                auditMetadata
        );
        auditEventRepository.save(auditEvent);

        log.info("Workflow action executed: action={} appRef={} fromStage={} toStage={} actor={}",
                action, savedApp.getApplicationReference(), fromStage, toStage, effectiveActor);

        return assembleApplicationResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationDetails(String idOrReference) {
        Application app = findApplication(idOrReference);
        securityUtils.assertInstitutionAccess(app.getInstitution());
        securityUtils.assertRegionAccess(app.getInstitution().getRegion());
        return assembleApplicationResponse(app);
    }

    public ApplicationResponse assembleApplicationResponse(Application app) {
        List<WorkflowTimelineItemResponse> timeline = getTimeline(app.getId());
        List<DeficiencyResponse> defs = deficiencyRepository.findByApplicationIdOrderByCreatedAtAsc(app.getId())
                .stream().map(DeficiencyResponse::from).toList();
        List<SubmissionResponse> subs = submissionRepository.findByApplicationIdOrderByVersionNumberDesc(app.getId())
                .stream().map(SubmissionResponse::from).toList();
        List<ChecklistItemResponse> chk = checklistRepository.findByApplicationIdOrderByItemCodeAsc(app.getId())
                .stream().map(ChecklistItemResponse::from).toList();

        return ApplicationResponse.from(app, timeline, defs, subs, chk);
    }

    public List<WorkflowTimelineItemResponse> getTimeline(Long applicationId) {
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
        if (transitions.isEmpty()) {
            return Collections.emptyList();
        }
        return transitions.stream().map(WorkflowTimelineItemResponse::from).toList();
    }

    public Application findApplication(String idOrReference) {
        if (idOrReference == null || idOrReference.isBlank()) {
            throw new ResourceNotFoundException("Application identifier cannot be empty.");
        }

        String trimmed = idOrReference.trim();
        if (trimmed.matches("\\d+")) {
            Long numericId = Long.parseLong(trimmed);
            return applicationRepository.findById(numericId)
                    .orElseGet(() -> applicationRepository.findByApplicationReference(trimmed)
                            .orElseThrow(() -> new ResourceNotFoundException("Application not found with identifier: " + trimmed)));
        }

        return applicationRepository.findByApplicationReference(trimmed)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with reference: " + trimmed));
    }
}
