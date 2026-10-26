package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.application.checklist.ApplicationChecklistRepository;
import in.gov.aicte.platform.application.deficiency.ApplicationDeficiency;
import in.gov.aicte.platform.application.deficiency.ApplicationDeficiencyRepository;
import in.gov.aicte.platform.application.submission.ApplicationSubmission;
import in.gov.aicte.platform.application.submission.ApplicationSubmissionRepository;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import in.gov.aicte.platform.workflow.WorkflowTransition;
import in.gov.aicte.platform.workflow.WorkflowTransitionRepository;
import in.gov.aicte.platform.workflow.dto.WorkflowActionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScrutinyWorkflowLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDeficiencyRepository deficiencyRepository;

    @Autowired
    private ApplicationSubmissionRepository submissionRepository;

    @Autowired
    private ApplicationChecklistRepository checklistRepository;

    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;

    private Institution institution;

    @BeforeEach
    void setup() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        institution = institutionRepository.save(new Institution(
                "INST-TEST-" + uid,
                "National Institute of Technology " + uid
        ));
    }

    @Test
    @WithMockUser
    @DisplayName("Phase 2: Complete Scrutiny & Deficiency Correction Lifecycle with Re-Scrutiny Resolution")
    void shouldExecuteEndToEndScrutinyLifecycle() throws Exception {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String appRef = "APP-TEST-" + uid;

        // 1. Institution creates application in DRAFT status
        Application app = new Application(
                institution,
                appRef,
                "Extension of Approval (EoA)",
                "2026-2027",
                "DRAFT"
        );
        app.setCurrentStage("Scrutiny");
        Application savedApp = applicationRepository.save(app);

        // Verify initial submission was created
        submissionRepository.save(new ApplicationSubmission(
                savedApp,
                "SUB-" + savedApp.getId() + "-v1",
                "INITIAL_DOSSIER",
                1,
                "USR-INST-01",
                "Dr. Ramesh Kumar",
                "Initial dossier submission",
                "SUBMITTED"
        ));

        // 2. Submit application transitions from DRAFT -> In progress
        WorkflowActionRequest submitReq = new WorkflowActionRequest(
                "SUBMIT_APPLICATION",
                "Dr. Ramesh Kumar",
                "Institution Representative",
                "Submitting completed institutional dossier for review.",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("In progress"))
                .andExpect(jsonPath("$.currentStage").value("Scrutiny"))
                .andExpect(jsonPath("$.ownerName").value("B. Singh (Scrutiny Officer)"));

        // 3. Scrutiny Officer updates a checklist item
        WorkflowActionRequest chkReq = new WorkflowActionRequest(
                "UPDATE_CHECKLIST",
                "B. Singh",
                "Scrutiny Officer",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Land title verified authentic",
                "CHK-LAND-01",
                true
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chkReq)))
                .andExpect(status().isOk());

        // 4. Scrutiny Officer returns application with itemized deficiencies
        WorkflowActionRequest returnReq = new WorkflowActionRequest(
                "RETURN_FOR_CORRECTION",
                "B. Singh",
                "Scrutiny Officer",
                "Faculty cadre ratio non-compliant; municipal fire safety NOC endorsement missing.",
                "Mandatory statutory deficiencies identified.",
                List.of(
                        "Faculty cadre ratio in Computer Engineering is 1:24 (APH norm: 1:15)",
                        "Municipal building and fire safety NOC endorsement certificate expired"
                ),
                null
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Returned"))
                .andExpect(jsonPath("$.currentStage").value("Re-scrutiny"))
                .andExpect(jsonPath("$.ownerName").value("Institution Representative"));

        // Verify deficiencies are persisted with status OPEN
        List<ApplicationDeficiency> openDefs = deficiencyRepository.findByApplicationIdOrderByCreatedAtAsc(savedApp.getId());
        assertThat(openDefs).hasSize(2);
        assertThat(openDefs.get(0).getStatus()).isEqualTo("OPEN");
        assertThat(openDefs.get(1).getStatus()).isEqualTo("OPEN");

        // 5. Guard check: Cannot complete scrutiny while application is in 'Returned' status
        WorkflowActionRequest illegalCompleteReq = new WorkflowActionRequest(
                "COMPLETE_SCRUTINY",
                "B. Singh",
                "Scrutiny Officer",
                "Attempting premature clearance",
                null,
                null,
                "EVC scheduling"
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalCompleteReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Cannot complete scrutiny while application status is 'Returned'")));

        // 6. Institution submits rectifications & evidence (creates version 2 submission)
        WorkflowActionRequest resubmitReq = new WorkflowActionRequest(
                "SUBMIT_CORRECTIONS",
                "Dr. Ramesh Kumar",
                "Institution Representative",
                "Appointed 3 additional Assistant Professors and attached valid Fire NOC acknowledgement receipt.",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resubmitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("In progress"))
                .andExpect(jsonPath("$.currentStage").value("Re-scrutiny"))
                .andExpect(jsonPath("$.ownerName").value("B. Singh (Scrutiny Officer)"));

        // Verify submission package version 2 was created
        List<ApplicationSubmission> subs = submissionRepository.findByApplicationIdOrderByVersionNumberDesc(savedApp.getId());
        assertThat(subs).hasSize(2);
        assertThat(subs.get(0).getVersionNumber()).isEqualTo(2);
        assertThat(subs.get(0).getSubmissionType()).isEqualTo("RECTIFICATION_PACKAGE");

        // Verify deficiencies transitioned to RECTIFIED_PENDING_REVIEW (NOT automatically resolved)
        List<ApplicationDeficiency> pendingDefs = deficiencyRepository.findByApplicationIdOrderByCreatedAtAsc(savedApp.getId());
        assertThat(pendingDefs).allMatch(d -> "RECTIFIED_PENDING_REVIEW".equals(d.getStatus()));
        assertThat(pendingDefs.get(0).getInstitutionResponse()).contains("Appointed 3 additional Assistant Professors");

        // 7. Guard check: Officer cannot clear scrutiny while deficiencies are still in RECTIFIED_PENDING_REVIEW
        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalCompleteReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("deficiency item(s) remain unresolved")));

        // 8. Re-Scrutiny: Officer reviews and resolves each deficiency individually
        for (ApplicationDeficiency def : pendingDefs) {
            WorkflowActionRequest resolveReq = new WorkflowActionRequest(
                    "RESOLVE_DEFICIENCY",
                    "B. Singh",
                    "Scrutiny Officer",
                    "Proofs scrutinized and verified compliant.",
                    null,
                    null,
                    null,
                    null,
                    def.getId(),
                    "RESOLVED",
                    "Appointment orders and Municipal NOC certified.",
                    null,
                    null
            );

            mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resolveReq)))
                    .andExpect(status().isOk());
        }

        // Verify all deficiencies are now RESOLVED
        List<ApplicationDeficiency> resolvedDefs = deficiencyRepository.findByApplicationIdOrderByCreatedAtAsc(savedApp.getId());
        assertThat(resolvedDefs).allMatch(d -> "RESOLVED".equals(d.getStatus()));

        // 9. Scrutiny clearance: Transition to next stage (Regional Committee Review / EVC-Exempt)
        WorkflowActionRequest completeReq = new WorkflowActionRequest(
                "COMPLETE_SCRUTINY",
                "B. Singh",
                "Scrutiny Officer",
                "All items verified and compliant with APH 2026-27 Table 2 norms.",
                "Application is EVC-exempt; forwarding to Regional Committee Review.",
                null,
                "Committee review"
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Due soon"))
                .andExpect(jsonPath("$.currentStage").value("Committee review"))
                .andExpect(jsonPath("$.ownerName").value("Regional Committee Chair"));

        // 10. Verify full audit trail and details endpoint
        mockMvc.perform(get("/api/v1/applications/{id}", savedApp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissions").isArray())
                .andExpect(jsonPath("$.submissions.length()").value(2))
                .andExpect(jsonPath("$.deficiencyItems").isArray())
                .andExpect(jsonPath("$.deficiencyItems.length()").value(2))
                .andExpect(jsonPath("$.timeline").isArray());

        List<WorkflowTransition> transitions = workflowTransitionRepository.findByApplicationIdOrderByCreatedAtDesc(savedApp.getId());
        assertThat(transitions).isNotEmpty();
    }

    @Test
    @WithMockUser
    @DisplayName("Phase 2: Validation of invalid workflow actions and missing reasons")
    void shouldRejectInvalidWorkflowActions() throws Exception {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Application app = applicationRepository.save(new Application(
                institution,
                "APP-INV-" + uid,
                "Extension of Approval (EoA)",
                "2026-2027",
                "DRAFT"
        ));

        // 1. Attempting SUBMIT_CORRECTIONS when status is DRAFT should fail
        WorkflowActionRequest invalidSubmitCorr = new WorkflowActionRequest(
                "SUBMIT_CORRECTIONS",
                "Dr. Ramesh Kumar",
                "Institution Representative",
                "Premature correction submission",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", app.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSubmitCorr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Status must be 'Returned'")));

        // 2. Attempting RETURN_FOR_CORRECTION with blank reason should fail
        app.setStatus("In progress");
        app.setCurrentStage("Scrutiny");
        applicationRepository.save(app);

        WorkflowActionRequest blankReasonReq = new WorkflowActionRequest(
                "RETURN_FOR_CORRECTION",
                "B. Singh",
                "Scrutiny Officer",
                "   ",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", app.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReasonReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("mandatory reason is required")));
    }
}
