package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.auth.DevTokenService;
import in.gov.aicte.platform.auth.dto.TokenRequest;
import in.gov.aicte.platform.auth.dto.TokenResponse;
import in.gov.aicte.platform.evc.EvcVisit;
import in.gov.aicte.platform.evc.EvcVisitRepository;
import in.gov.aicte.platform.evc.dto.CompleteVisitRequest;
import in.gov.aicte.platform.evc.dto.RescheduleEvcRequest;
import in.gov.aicte.platform.evc.dto.ScheduleEvcRequest;
import in.gov.aicte.platform.evc.dto.SubmitEvcReportRequest;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import in.gov.aicte.platform.workflow.WorkflowService;
import in.gov.aicte.platform.workflow.dto.WorkflowActionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EvcOperationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EvcVisitRepository evcVisitRepository;

    @Autowired
    private DevTokenService devTokenService;

    @Autowired
    private WorkflowService workflowService;

    private String evcToken;

    @BeforeEach
    void setUp() {
        TokenResponse tokenResp = devTokenService.issueToken("evc.coord@aicte-india.gov.in", "EVC Coordinator");
        this.evcToken = "Bearer " + tokenResp.access_token();
    }

    private Institution createSampleInstitution(String suffix) {
        Institution inst = new Institution(
                "INST-" + suffix,
                "Apex Institute of Technology " + suffix,
                "Northern Region"
        );
        return institutionRepository.save(inst);
    }

    private Application createSampleApplication(Institution inst, String stage, String status) {
        Application app = new Application(
                inst,
                "APP-2026-" + UUID.randomUUID().toString().substring(0, 8),
                "Extension of Approval (EoA)",
                "2026-2027",
                status,
                stage,
                "C. Verma (EVC Coordinator)",
                "USR-EVC-01",
                LocalDate.now().plusDays(10),
                "Scheduled for verification"
        );
        return applicationRepository.save(app);
    }

    @Test
    @DisplayName("GET /api/v1/evc returns list of EVC cases with valid response mapping")
    void testListEvcVisits() throws Exception {
        Institution inst = createSampleInstitution("TEST-LIST");
        Application app = createSampleApplication(inst, "EVC scheduling", "In progress");

        EvcVisit visit = new EvcVisit(app, "EVC-" + app.getId(), "C. Verma", "USR-EVC-01");
        evcVisitRepository.save(visit);

        mockMvc.perform(get("/api/v1/evc")
                        .header("Authorization", evcToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.evcReference == '" + visit.getEvcReference() + "')]").exists());
    }

    @Test
    @DisplayName("GET /api/v1/evc/metrics returns all 6 KPIs correctly")
    void testGetEvcMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/evc/metrics")
                        .header("Authorization", evcToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").isNumber())
                .andExpect(jsonPath("$.pendingScheduling").isNumber())
                .andExpect(jsonPath("$.upcomingVisits").isNumber())
                .andExpect(jsonPath("$.completedVisits").isNumber())
                .andExpect(jsonPath("$.reportsPending").isNumber())
                .andExpect(jsonPath("$.overdueTasks").isNumber());
    }

    @Test
    @DisplayName("Full EVC Lifecycle: Schedule -> Complete Visit -> Submit Report -> Advances Application to Committee review")
    void testFullEvcLifecycle() throws Exception {
        Institution inst = createSampleInstitution("LIFECYCLE");
        Application app = createSampleApplication(inst, "EVC scheduling", "Due soon");

        EvcVisit visit = new EvcVisit(app, "EVC-" + app.getId(), "C. Verma", "USR-EVC-01");
        visit.setStatus("Awaiting scheduling");
        visit = evcVisitRepository.save(visit);

        String evcRef = visit.getEvcReference();

        // 1. Schedule Visit
        ScheduleEvcRequest scheduleReq = new ScheduleEvcRequest(
                LocalDate.now().plusDays(5),
                "C. Verma",
                "Physical On-Site Verification",
                List.of("Prof. R. Sharma (IIT Delhi)", "Dr. S. K. Gupta (NIT Kurukshetra)"),
                "C. Verma",
                "EVC Coordinator"
        );

        mockMvc.perform(post("/api/v1/evc/" + evcRef + "/schedule")
                        .header("Authorization", evcToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Scheduled"))
                .andExpect(jsonPath("$.visitDate").value(scheduleReq.visitDate().toString()))
                .andExpect(jsonPath("$.experts.length()").value(2));

        // Verify in DB
        EvcVisit scheduledVisit = evcVisitRepository.findByEvcReference(evcRef).orElseThrow();
        assertThat(scheduledVisit.getStatus()).isEqualTo("Scheduled");
        assertThat(scheduledVisit.getVisitDate()).isEqualTo(scheduleReq.visitDate());

        // 2. Mark Visit Completed
        CompleteVisitRequest completeReq = new CompleteVisitRequest(
                "Inspection completed. Labs and faculty verified without critical deficiency.",
                "C. Verma",
                "EVC Coordinator"
        );

        mockMvc.perform(post("/api/v1/evc/" + evcRef + "/complete")
                        .header("Authorization", evcToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Completed"))
                .andExpect(jsonPath("$.reportStatus").value("Report pending submission"))
                .andExpect(jsonPath("$.completionNotes").value(completeReq.completionNotes()));

        // 3. Submit Final EVC Report -> Must advance linked Application to 'Committee review'
        SubmitEvcReportRequest reportReq = new SubmitEvcReportRequest(
                "Recommended Approval",
                "All AICTE physical infrastructure norms satisfied as per APH 2026-27.",
                "C. Verma",
                "EVC Coordinator"
        );

        mockMvc.perform(post("/api/v1/evc/" + evcRef + "/report")
                        .header("Authorization", evcToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Complete for stage"))
                .andExpect(jsonPath("$.reportStatus").value("Report Submitted & Verified"))
                .andExpect(jsonPath("$.reportOutcome").value("Recommended Approval"));

        // Verify linked application has advanced to Committee review
        Application updatedApp = applicationRepository.findById(app.getId()).orElseThrow();
        assertThat(updatedApp.getCurrentStage()).isEqualTo("Committee review");
        assertThat(updatedApp.getStatus()).isEqualTo("In progress");
        assertThat(updatedApp.getOwnerName()).contains("Committee");
    }

    @Test
    @DisplayName("Reschedule / Revert: Can revert completed visit back to scheduled")
    void testRescheduleRevert() throws Exception {
        Institution inst = createSampleInstitution("REVERT");
        Application app = createSampleApplication(inst, "EVC scheduling", "In progress");

        EvcVisit visit = new EvcVisit(app, "EVC-" + app.getId(), "C. Verma", "USR-EVC-01");
        visit.setStatus("Completed");
        visit = evcVisitRepository.save(visit);

        RescheduleEvcRequest reschedReq = new RescheduleEvcRequest(
                LocalDate.now().plusDays(7),
                "C. Verma",
                "EVC Coordinator"
        );

        mockMvc.perform(post("/api/v1/evc/" + visit.getEvcReference() + "/reschedule")
                        .header("Authorization", evcToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Scheduled"))
                .andExpect(jsonPath("$.visitDate").value(reschedReq.newVisitDate().toString()));
    }

    @Test
    @DisplayName("Workflow Integration: Clearing Scrutiny automatically creates an EVC work item")
    void testScrutinyClearanceCreatesEvcWorkItem() {
        Institution inst = createSampleInstitution("AUTO-EVC");
        Application app = createSampleApplication(inst, "Scrutiny", "In progress");

        WorkflowActionRequest clearReq = new WorkflowActionRequest(
                "COMPLETE_SCRUTINY",
                "B. Singh",
                "Scrutiny Officer",
                "Documents verified. Forwarding for physical EVC inspection.",
                "All mandatory statutory approvals present.",
                null,
                "EVC scheduling"
        );

        workflowService.executeAction(app.getApplicationReference(), clearReq, "USR-SO-01");

        // Verify application transitioned
        Application updated = applicationRepository.findById(app.getId()).orElseThrow();
        assertThat(updated.getCurrentStage()).isEqualTo("EVC scheduling");

        // Verify EVC visit record created
        EvcVisit visit = evcVisitRepository.findByApplicationId(app.getId()).orElse(null);
        assertThat(visit).isNotNull();
        assertThat(visit.getStatus()).isEqualTo("Awaiting scheduling");
        assertThat(visit.getEvcReference()).isEqualTo("EVC-" + app.getId());
    }
}
