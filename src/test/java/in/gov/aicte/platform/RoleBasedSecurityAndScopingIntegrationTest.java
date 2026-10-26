package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.application.dto.CreateApplicationRequest;
import in.gov.aicte.platform.auth.DevTokenService;
import in.gov.aicte.platform.auth.dto.TokenResponse;
import in.gov.aicte.platform.evc.dto.ScheduleEvcRequest;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleBasedSecurityAndScopingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private DevTokenService devTokenService;

    private String northInstToken;
    private String southInstToken;
    private String northRoToken;
    private String southRoToken;
    private String scrutinyToken;
    private String evcToken;
    private String adminToken;

    private Institution northInstitution;
    private Institution southInstitution;
    private Application northApplication;
    private Application southApplication;

    @BeforeEach
    void setUp() {
        // Issue tokens for distinct roles, institutions, and regions
        northInstToken = "Bearer " + devTokenService.issueToken("principal@abctech.edu.in", "Institution Representative").access_token();
        southInstToken = "Bearer " + devTokenService.issueToken("principal@southpoly.edu.in", "Institution Representative").access_token();
        northRoToken = "Bearer " + devTokenService.issueToken("ro.north@aicte-india.gov.in", "Regional Officer").access_token();
        southRoToken = "Bearer " + devTokenService.issueToken("ro.south@aicte-india.gov.in", "Regional Officer").access_token();
        scrutinyToken = "Bearer " + devTokenService.issueToken("scrutiny.north@aicte-india.gov.in", "Scrutiny Officer").access_token();
        evcToken = "Bearer " + devTokenService.issueToken("evc.coord@aicte-india.gov.in", "EVC Coordinator").access_token();
        adminToken = "Bearer " + devTokenService.issueToken("admin@aicte-india.gov.in", "Administrator").access_token();

        // Seed Northern Institution (INST-1025)
        northInstitution = institutionRepository.findByInstitutionalIdentifier("INST-1025")
                .orElseGet(() -> institutionRepository.save(new Institution("INST-1025", "ABC Institute of Technology", "Northern Region")));

        // Seed Southern Institution (INST-2048)
        southInstitution = institutionRepository.findByInstitutionalIdentifier("INST-2048")
                .orElseGet(() -> institutionRepository.save(new Institution("INST-2048", "Southern Polytechnic College", "Southern Region")));

        String uid = UUID.randomUUID().toString().substring(0, 6);

        // Seed Northern Application
        northApplication = new Application(
                northInstitution,
                "APP-NORTH-" + uid,
                "Extension of Approval (EoA)",
                "2026-2027",
                "Submitted"
        );
        northApplication.setCurrentStage("Scrutiny");
        northApplication = applicationRepository.save(northApplication);

        // Seed Southern Application
        southApplication = new Application(
                southInstitution,
                "APP-SOUTH-" + uid,
                "New Technical Institution",
                "2026-2027",
                "Submitted"
        );
        southApplication.setCurrentStage("Scrutiny");
        southApplication = applicationRepository.save(southApplication);
    }

    @Test
    @DisplayName("SEC-01: Unauthenticated requests to protected endpoints return 401 Unauthorized")
    void shouldRejectUnauthenticatedRequestsWith401() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institutionalIdentifier\": \"INST-TEST\", \"name\": \"Test Inst\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-02: Endpoint Authorization: Institution Representative cannot schedule EVC visits (403)")
    void shouldRejectEvcScheduleByInstitutionRepresentative() throws Exception {
        ScheduleEvcRequest request = new ScheduleEvcRequest(
                LocalDate.now().plusDays(7),
                "Dr. Unauthorized",
                "Physical On-Site",
                List.of("Expert 1"),
                "Dr. Ramesh Kumar",
                "Institution Representative"
        );

        mockMvc.perform(post("/api/v1/evc/" + northApplication.getId() + "/schedule")
                        .header("Authorization", northInstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value("https://api.aicte.gov.in/errors/forbidden"));
    }

    @Test
    @DisplayName("SEC-03: Endpoint Authorization: Scrutiny Officer cannot register new applications (403)")
    void shouldRejectApplicationCreationByScrutinyOfficer() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest(
                northInstitution.getId(),
                "APP-NEW-" + UUID.randomUUID().toString().substring(0, 6),
                "Extension of Approval (EoA)",
                "2026-2027"
        );

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", scrutinyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("SEC-04: Workflow Action Authorization: Institution Representative cannot execute RETURN_FOR_CORRECTION (403)")
    void shouldRejectReturnActionByInstitutionRepresentative() throws Exception {
        WorkflowActionRequest returnReq = new WorkflowActionRequest(
                "RETURN_FOR_CORRECTION",
                "Dr. Ramesh Kumar",
                "Institution Representative",
                "Attempting unauthorized self-return",
                null,
                List.of("DEF-01: Fake deficiency"),
                null
        );

        mockMvc.perform(post("/api/v1/applications/" + northApplication.getId() + "/actions")
                        .header("Authorization", northInstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("Unauthorized workflow transition: Action 'RETURN_FOR_CORRECTION'")));
    }

    @Test
    @DisplayName("SEC-05: Institutional Tenant Isolation: Rep cannot view application of another institution (403)")
    void shouldRejectCrossInstitutionApplicationView() throws Exception {
        // Northern Inst Rep attempts to view Southern Application
        mockMvc.perform(get("/api/v1/applications/" + southApplication.getId())
                        .header("Authorization", northInstToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("Cross-institution access violation")));
    }

    @Test
    @DisplayName("SEC-06: Institutional Tenant Isolation: Rep cannot create application for another institution (403)")
    void shouldRejectCrossInstitutionApplicationCreation() throws Exception {
        // Northern Inst Rep attempts to create application for Southern Institution (INST-2048)
        CreateApplicationRequest request = new CreateApplicationRequest(
                southInstitution.getId(),
                "APP-CROSS-" + UUID.randomUUID().toString().substring(0, 6),
                "Extension of Approval (EoA)",
                "2026-2027"
        );

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", northInstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("Cross-institution access violation")));
    }

    @Test
    @DisplayName("SEC-07: Institutional Tenant Isolation: Rep application list automatically scoped to their own institution")
    void shouldAutomaticallyScopeApplicationListForInstitutionRep() throws Exception {
        // Northern Inst Rep lists applications
        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", northInstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[*].institution.institutionalIdentifier", everyItem(containsString("INST-1025"))));
    }

    @Test
    @DisplayName("SEC-08: Territorial Regional Isolation: Northern Regional Officer cannot view Southern Application (403)")
    void shouldRejectCrossRegionApplicationView() throws Exception {
        // Northern Regional Officer attempts to view Southern Application
        mockMvc.perform(get("/api/v1/applications/" + southApplication.getId())
                        .header("Authorization", northRoToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("Cross-region access violation")));
    }

    @Test
    @DisplayName("SEC-09: Platform Administrator possesses unrestricted multi-tenant and multi-region access")
    void shouldAllowAdministratorAccessAcrossAllRegionsAndInstitutions() throws Exception {
        // Admin views Northern Application
        mockMvc.perform(get("/api/v1/applications/" + northApplication.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationReference").value(northApplication.getApplicationReference()));

        // Admin views Southern Application
        mockMvc.perform(get("/api/v1/applications/" + southApplication.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationReference").value(southApplication.getApplicationReference()));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("SEC-05: Generic USER role cannot access governance API resources")
    void shouldRejectGenericUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isForbidden());
    }

}
