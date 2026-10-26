package in.gov.aicte.platform.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.checklist.ApplicationChecklist;
import in.gov.aicte.platform.application.checklist.ApplicationChecklistRepository;
import in.gov.aicte.platform.application.dto.ApplicationResponse;
import in.gov.aicte.platform.application.dto.CreateApplicationRequest;
import in.gov.aicte.platform.application.submission.ApplicationSubmission;
import in.gov.aicte.platform.application.submission.ApplicationSubmissionRepository;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.common.exception.DuplicateResourceException;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import in.gov.aicte.platform.common.pagination.PageResponse;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import in.gov.aicte.platform.workflow.WorkflowService;
import in.gov.aicte.platform.workflow.dto.WorkflowActionRequest;
import in.gov.aicte.platform.workflow.dto.WorkflowTimelineItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final InstitutionRepository institutionRepository;
    private final AuditEventRepository auditEventRepository;
    private final ApplicationChecklistRepository checklistRepository;
    private final ApplicationSubmissionRepository submissionRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowService workflowService;
    private final in.gov.aicte.platform.auth.SecurityUtils securityUtils;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            InstitutionRepository institutionRepository,
            AuditEventRepository auditEventRepository,
            ApplicationChecklistRepository checklistRepository,
            ApplicationSubmissionRepository submissionRepository,
            ObjectMapper objectMapper,
            WorkflowService workflowService,
            in.gov.aicte.platform.auth.SecurityUtils securityUtils
    ) {
        this.applicationRepository = applicationRepository;
        this.institutionRepository = institutionRepository;
        this.auditEventRepository = auditEventRepository;
        this.checklistRepository = checklistRepository;
        this.submissionRepository = submissionRepository;
        this.objectMapper = objectMapper;
        this.workflowService = workflowService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request, String actorId) {
        String reference = request.applicationReference().trim();

        if (applicationRepository.existsByApplicationReference(reference)) {
            throw new DuplicateResourceException("Application already exists with reference: " + reference);
        }

        Institution institution = institutionRepository.findById(request.institutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + request.institutionId()));

        // Enforce tenant scoping: Institution Representative cannot create application for another institution
        securityUtils.assertInstitutionAccess(institution);

        Application application = new Application(
                institution,
                reference,
                request.applicationType().trim(),
                request.academicYear().trim(),
                "DRAFT" // Initial prototype status
        );
        Application saved = applicationRepository.save(application);

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";

        // Seed applicable statutory checklist for this application category
        checklistRepository.save(new ApplicationChecklist(saved, "CHK-LAND-01", "Mandatory land title deed & non-encumbrance certificate verified", "APH Table 1.1 Norms", false, null));
        checklistRepository.save(new ApplicationChecklist(saved, "CHK-FACULTY-02", "Faculty cadre ratio meets APH Table 2.1 norms (1:15 / 1:20)", "APH Table 2.1 Norms", false, null));
        checklistRepository.save(new ApplicationChecklist(saved, "CHK-FIRE-03", "Municipal building plan & fire safety NOC endorsement certified", "APH Norms Clause 3.4", false, null));
        checklistRepository.save(new ApplicationChecklist(saved, "CHK-LAB-04", "Laboratory and computing facilities verified compliant with AICTE norms", "APH Norms Clause 4.2", false, null));

        // Create initial submission version
        ApplicationSubmission sub = new ApplicationSubmission(
                saved,
                "SUB-" + saved.getId() + "-v1",
                "INITIAL_DOSSIER",
                1,
                effectiveActor,
                "Institution Representative",
                "Application registered and initial statutory dossier submitted.",
                "SUBMITTED"
        );
        submissionRepository.save(sub);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "CREATE_APPLICATION");
        metadata.put("applicationReference", saved.getApplicationReference());
        metadata.put("institutionId", institution.getId());
        metadata.put("applicationType", saved.getApplicationType());
        metadata.put("academicYear", saved.getAcademicYear());

        String auditMetadata;
        try {
            auditMetadata = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit metadata for application id={}", saved.getId(), e);
            auditMetadata = "{}";
        }

        AuditEvent auditEvent = new AuditEvent(
                "APPLICATION_CREATED",
                effectiveActor,
                "APPLICATION",
                saved.getId().toString(),
                auditMetadata
        );
        auditEventRepository.save(auditEvent);

        log.info("Application created id={} reference='{}' for institutionId={} by actor='{}'",
                saved.getId(), saved.getApplicationReference(), institution.getId(), effectiveActor);
        return workflowService.assembleApplicationResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        return workflowService.getApplicationDetails(id.toString());
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationByIdOrReference(String idOrReference) {
        return workflowService.getApplicationDetails(idOrReference);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listApplications(
            Long institutionId,
            String status,
            String academicYear,
            Pageable pageable
    ) {
        return listApplications(institutionId, status, academicYear, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listApplications(
            Long institutionId,
            String status,
            String academicYear,
            String region,
            String stage,
            String search,
            Pageable pageable
    ) {
        Long effectiveInstitutionId = institutionId;
        String effectiveRegion = region;

        if (securityUtils.hasRole(in.gov.aicte.platform.auth.SecurityUtils.ROLE_INSTITUTION_REPRESENTATIVE)) {
            java.util.Optional<in.gov.aicte.platform.auth.dto.DevUserDto> userOpt = securityUtils.getCurrentUser();
            if (userOpt.isPresent() && userOpt.get().institutionId() != null) {
                String instIdent = userOpt.get().institutionId().trim();
                java.util.Optional<Institution> instOpt = institutionRepository.findByInstitutionalIdentifier(instIdent);
                if (instOpt.isPresent()) {
                    effectiveInstitutionId = instOpt.get().getId();
                } else if (instIdent.matches("\\d+")) {
                    effectiveInstitutionId = Long.parseLong(instIdent);
                }
            }
        } else if (securityUtils.hasRole(in.gov.aicte.platform.auth.SecurityUtils.ROLE_REGIONAL_OFFICER)) {
            java.util.Optional<in.gov.aicte.platform.auth.dto.DevUserDto> userOpt = securityUtils.getCurrentUser();
            if (userOpt.isPresent() && userOpt.get().region() != null && (effectiveRegion == null || effectiveRegion.isBlank())) {
                String r = userOpt.get().region().trim();
                if (!"All India".equalsIgnoreCase(r) && !"Multi-Region".equalsIgnoreCase(r) && !"Platform-wide".equalsIgnoreCase(r)) {
                    effectiveRegion = r;
                }
            }
        }

        Page<Application> page = applicationRepository.findAll(
                ApplicationSpecifications.withFilters(effectiveInstitutionId, status, academicYear, effectiveRegion, stage, search),
                pageable
        );
        return PageResponse.from(page.map(workflowService::assembleApplicationResponse));
    }

    @Transactional
    public ApplicationResponse executeWorkflowAction(String idOrReference, WorkflowActionRequest request, String actorId) {
        return workflowService.executeAction(idOrReference, request, actorId);
    }
}
