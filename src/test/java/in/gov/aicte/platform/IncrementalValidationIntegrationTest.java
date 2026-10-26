package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.auth.DevTokenService;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import in.gov.aicte.platform.validation.ValidationProvenanceRepository;
import in.gov.aicte.platform.validation.dto.EvaluateApplicationRequest;
import in.gov.aicte.platform.validation.dto.ValidationProvenanceResponse;
import in.gov.aicte.platform.validation.dto.ValidationReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncrementalValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ValidationProvenanceRepository provenanceRepository;

    @Autowired
    private DevTokenService tokenService;

    private Application testApp;
    private Institution testInst;
    private String adminToken;
    private String instRepToken;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testInst = new Institution();
        testInst.setInstitutionalIdentifier("INST-VAL-" + uid);
        testInst.setName("Apex Institute of Technology - " + uid);
        testInst.setRegion("Northern Region");
        testInst = institutionRepository.save(testInst);

        testApp = new Application();
        testApp.setApplicationReference("APP-VAL-" + uid);
        testApp.setInstitution(testInst);
        testApp.setApplicationType("Extension of Approval (EoA)");
        testApp.setAcademicYear("2026-2027");
        testApp.setCurrentStage("Scrutiny");
        testApp.setStatus("IN_PROGRESS");
        testApp.setOwnerId("USR-INST-01");
        testApp.setOwnerName("Apex Representative");
        testApp = applicationRepository.save(testApp);

        adminToken = tokenService.issueToken("USR-ADM-01", "Administrator").access_token();
        instRepToken = tokenService.issueToken("USR-INST-01", "Institution Representative").access_token();
    }

    @Test
    @DisplayName("Initial evaluation runs all rules and establishes baseline provenance")
    void testInitialEvaluationCreatesProvenance() throws Exception {
        EvaluateApplicationRequest req = new EvaluateApplicationRequest(Map.of(), false, "Admin Officer", "Administrator");

        MvcResult result = mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationReference").value(testApp.getApplicationReference()))
                .andExpect(jsonPath("$.totalRulesCount").isNumber())
                .andExpect(jsonPath("$.rulesReevaluatedCount").value(8))
                .andExpect(jsonPath("$.rulesSkippedCount").value(0))
                .andExpect(jsonPath("$.statutoryMandatoryPassed").value(true))
                .andReturn();

        ValidationReportResponse report = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationReportResponse.class);
        assertThat(report.activeProofs()).hasSize(8);
        assertThat(report.dependencyGraph()).containsKey("RULE-LAND-01");
        assertThat(report.dependencyGraph()).containsKey("RULE-FACULTY-03");
    }

    @Test
    @DisplayName("Subsequent evaluation without modifications skips all rules (Unmodified Cache Hit)")
    void testSubsequentEvaluationSkipsAllUnmodifiedRules() throws Exception {
        // Run initial evaluation
        mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Immediately evaluate again with no changes
        MvcResult secondResult = mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rulesSkippedCount").value(8))
                .andExpect(jsonPath("$.rulesReevaluatedCount").value(0))
                .andReturn();

        ValidationReportResponse report = objectMapper.readValue(secondResult.getResponse().getContentAsString(), ValidationReportResponse.class);
        assertThat(report.rulesSkippedCount()).isEqualTo(8);
        assertThat(report.rulesReevaluatedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Modifying faculty roster triggers dirty check and re-evaluates only dirty/dependent rules")
    void testModifyingFieldReevaluatesOnlyAffectedRules() throws Exception {
        // Run baseline evaluation
        mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Now modify faculty roster
        EvaluateApplicationRequest dirtyRequest = new EvaluateApplicationRequest(
                Map.of("faculty_roster", "Updated roster with 40 professors and 80 assistant professors"),
                false,
                "Apex Rep",
                "Institution Representative"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dirtyRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ValidationReportResponse report = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationReportResponse.class);

        // Independent rules (Land, Fire NOC, Builtup, Occupancy, Lab, Library) are SKIPPED
        assertThat(report.rulesSkippedCount()).isGreaterThanOrEqualTo(6);

        // Faculty rule and AI readiness (which depends on faculty) were RE-EVALUATED
        ValidationProvenanceResponse facultyProof = report.activeProofs().stream()
                .filter(p -> "RULE-FACULTY-03".equals(p.ruleId()))
                .findFirst()
                .orElseThrow();
        assertThat(facultyProof.reEvaluationReason()).isEqualTo("FIELD_DIRTY_HASH_MISMATCH");

        ValidationProvenanceResponse landProof = report.activeProofs().stream()
                .filter(p -> "RULE-LAND-01".equals(p.ruleId()))
                .findFirst()
                .orElseThrow();
        // Land proof was untouched
        assertThat(landProof.reEvaluationReason()).isEqualTo("INITIAL_EVALUATION");
    }

    @Test
    @DisplayName("Modifying student intake triggers dependency invalidation for dependent rules")
    void testDependencyInvalidationTraversesGraph() throws Exception {
        // Run baseline evaluation
        mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Student intake is an upstream dependency for RULE-FACULTY-03 and RULE-LABORATORY-06
        EvaluateApplicationRequest depRequest = new EvaluateApplicationRequest(
                Map.of("student_intake", "Approved expansion to 720 students"),
                false,
                "Scrutiny Lead",
                "Scrutiny Officer"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ValidationReportResponse report = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationReportResponse.class);

        ValidationProvenanceResponse facultyProof = report.activeProofs().stream()
                .filter(p -> "RULE-FACULTY-03".equals(p.ruleId()))
                .findFirst().orElseThrow();
        assertThat(facultyProof.reEvaluationReason()).isEqualTo("DEPENDENCY_INVALIDATION");

        ValidationProvenanceResponse labProof = report.activeProofs().stream()
                .filter(p -> "RULE-LABORATORY-06".equals(p.ruleId()))
                .findFirst().orElseThrow();
        assertThat(labProof.reEvaluationReason()).isEqualTo("DEPENDENCY_INVALIDATION");

        // Land rule does not depend on student intake -> SKIPPED
        ValidationProvenanceResponse landProof = report.activeProofs().stream()
                .filter(p -> "RULE-LAND-01".equals(p.ruleId()))
                .findFirst().orElseThrow();
        assertThat(landProof.reEvaluationReason()).isEqualTo("INITIAL_EVALUATION");
    }

    @Test
    @DisplayName("AI recommendations are clearly distinguished from binding statutory decisions")
    void testAiRecommendationDistinction() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        ValidationReportResponse report = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationReportResponse.class);

        ValidationProvenanceResponse aiProof = report.activeProofs().stream()
                .filter(p -> "RULE-AI-READINESS-08".equals(p.ruleId()))
                .findFirst().orElseThrow();

        assertThat(aiProof.decisionType()).isEqualTo("AI_ASSISTED_ADVISORY");
        assertThat(aiProof.ruleCategory()).isEqualTo("ADVISORY_RECOMMENDED");
        assertThat(aiProof.proofDetails()).contains("Non-binding algorithmic assessment for officer guidance only");

        // Mandatory rules are DETERMINISTIC_RULE
        ValidationProvenanceResponse landProof = report.activeProofs().stream()
                .filter(p -> "RULE-LAND-01".equals(p.ruleId()))
                .findFirst().orElseThrow();
        assertThat(landProof.decisionType()).isEqualTo("DETERMINISTIC_RULE");
        assertThat(landProof.ruleCategory()).isEqualTo("STATUTORY_MANDATORY");
    }

    @Test
    @DisplayName("Provenance history endpoint returns full audit trail")
    void testProvenanceHistoryRetrieval() throws Exception {
        // 1. Initial
        mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // 2. Modify
        mockMvc.perform(post("/api/v1/validation/" + testApp.getId() + "/evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EvaluateApplicationRequest(
                                Map.of("land_details", "Updated parcel survey 204B"),
                                false,
                                "Admin",
                                "Administrator"
                        ))))
                .andExpect(status().isOk());

        // Query provenance history
        mockMvc.perform(get("/api/v1/validation/" + testApp.getId() + "/provenance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(9)));
    }

    @Test
    @DisplayName("Rules catalog lists all canonical rules with APH clauses")
    void testRulesCatalogEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/validation/rules")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleId").exists())
                .andExpect(jsonPath("$[0].aphClause").exists())
                .andExpect(jsonPath("$[0].ruleCategory").exists())
                .andExpect(jsonPath("$[0].dependentFields").isArray());
    }
}
