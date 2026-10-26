package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.auth.DevTokenService;
import in.gov.aicte.platform.auth.dto.TokenRequest;
import in.gov.aicte.platform.auth.dto.TokenResponse;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import in.gov.aicte.platform.workflow.WorkflowTransition;
import in.gov.aicte.platform.workflow.WorkflowTransitionRepository;
import in.gov.aicte.platform.workflow.dto.WorkflowActionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkflowAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Autowired
    private in.gov.aicte.platform.application.deficiency.ApplicationDeficiencyRepository deficiencyRepository;

    @Autowired
    private DevTokenService devTokenService;

    @Test
    @DisplayName("Dev Auth Bridge: POST /api/v1/auth/token generates bearer token that grants authenticated access")
    void shouldAuthenticateWithDevBearerToken() throws Exception {
        TokenRequest tokenRequest = new TokenRequest("scrutiny.north@aicte-india.gov.in", "anyPassword", "Scrutiny Officer");

        MvcResult tokenResult = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.user.role").value("Scrutiny Officer"))
                .andExpect(jsonPath("$.user.id").value("USR-SO-01"))
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(tokenResult.getResponse().getContentAsString(), TokenResponse.class);

        // Access protected endpoint using Bearer token
        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer " + tokenResponse.access_token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenResponse.access_token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("USR-SO-01"));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        TokenRequest invalidIdentity = new TokenRequest("unknown@example.invalid", "unused", "Administrator");
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidIdentity)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("Error Handling Regression: Malformed JSON request body returns 400 Bad Request ProblemDetail")
    void shouldReturn400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institutionalIdentifier\": \"BAD-JSON\", broken}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed JSON Request"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("Error Handling Regression: Parameter type mismatch returns 400 Bad Request ProblemDetail")
    void shouldReturn400ForParameterTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/applications")
                        .param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Parameter Type"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.parameter").value("page"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("Error Handling Regression: Unsupported HTTP method returns 405 Method Not Allowed")
    void shouldReturn405ForUnsupportedMethod() throws Exception {
        mockMvc.perform(delete("/api/v1/institutions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Method Not Allowed"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("Error Handling Regression: Unsupported media type returns 415 Unsupported Media Type")
    void shouldReturn415ForUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("some raw text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("JSON Escaping Regression: Names with quotes, backslashes, and line breaks serialize safely")
    void shouldHandleSpecialCharactersInAuditMetadataSafely() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String complexName = "St. Mary's \"National\" Institute\nDepartment of Tech\\Eng";

        mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new in.gov.aicte.platform.institution.dto.CreateInstitutionRequest(
                                "INST-ESC-" + unique,
                                complexName
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(complexName));
    }

    @Test
    @WithMockUser(username = "scrutiny_officer", roles = "ADMINISTRATOR")
    @DisplayName("End-to-End Workflow Transitions: RETURN_FOR_CORRECTION -> SUBMIT_CORRECTIONS -> COMPLETE_SCRUTINY")
    void shouldExecuteFullScrutinyLifecycleTransitions() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup institution and application in Scrutiny stage
        Institution inst = institutionRepository.save(new Institution("INST-WF-" + unique, "State Tech Campus " + unique, "Northern Region"));
        Application app = new Application(
                inst,
                "APP-WF-" + unique,
                "EXTENSION_OF_APPROVAL",
                "2026-2027",
                "In progress",
                "Scrutiny",
                "B. Singh (Scrutiny Officer)",
                "USR-SO-01",
                LocalDate.now().plusDays(5),
                "Initial checklist pending verification"
        );
        Application savedApp = applicationRepository.save(app);

        // 2. Action: RETURN_FOR_CORRECTION
        WorkflowActionRequest returnReq = new WorkflowActionRequest(
                "RETURN_FOR_CORRECTION",
                "B. Singh",
                "Scrutiny Officer",
                "Fire safety NOC expired; faculty ratio non-compliant.",
                "Checklist evaluated; returned for rectification.",
                List.of("Fire safety NOC expired", "Faculty ratio below 1:15"),
                null
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getApplicationReference())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Returned"))
                .andExpect(jsonPath("$.currentStage").value("Re-scrutiny"))
                .andExpect(jsonPath("$.ownerName").value("Institution Representative"))
                .andExpect(jsonPath("$.deficiencies").isArray())
                .andExpect(jsonPath("$.timeline[0].title").value("Deficiency Notice Issued · Returned for Correction"));

        // 3. Action: SUBMIT_CORRECTIONS
        WorkflowActionRequest resubmitReq = new WorkflowActionRequest(
                "SUBMIT_CORRECTIONS",
                "Dr. Ramesh Kumar",
                "Institution Representative",
                "Updated NOC renewal acknowledgement and revised faculty roster submitted.",
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
                .andExpect(jsonPath("$.ownerName").value("B. Singh (Scrutiny Officer)"))
                .andExpect(jsonPath("$.timeline[0].title").value("Corrections Resubmitted by Institution"));

        // Verify deficiencies are NOT erased; they are in RECTIFIED_PENDING_REVIEW status
        List<in.gov.aicte.platform.application.deficiency.ApplicationDeficiency> defs = deficiencyRepository.findByApplicationIdOrderByCreatedAtAsc(savedApp.getId());
        assertThat(defs).hasSize(2);
        assertThat(defs.get(0).getStatus()).isEqualTo("RECTIFIED_PENDING_REVIEW");
        assertThat(defs.get(1).getStatus()).isEqualTo("RECTIFIED_PENDING_REVIEW");

        // 4. Action: COMPLETE_SCRUTINY without resolving deficiencies should be REJECTED (400 Bad Request)
        WorkflowActionRequest completeReq = new WorkflowActionRequest(
                "COMPLETE_SCRUTINY",
                "B. Singh",
                "Scrutiny Officer",
                "All items verified and compliant.",
                "Forwarded to EVC scheduling desk.",
                null,
                "EVC scheduling"
        );

        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getApplicationReference())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("remain unresolved")));

        // 5. Authorized Scrutiny Officer resolves the deficiencies upon re-scrutiny
        for (in.gov.aicte.platform.application.deficiency.ApplicationDeficiency def : defs) {
            WorkflowActionRequest resolveReq = new WorkflowActionRequest(
                    "RESOLVE_DEFICIENCY",
                    "B. Singh",
                    "Scrutiny Officer",
                    "Verified rectified proofs against APH norms.",
                    null,
                    null,
                    null,
                    null,
                    def.getId(),
                    "RESOLVED",
                    "Document verified authentic and compliant.",
                    null,
                    null
            );
            mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getApplicationReference())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resolveReq)))
                    .andExpect(status().isOk());
        }

        // 6. NOW COMPLETE_SCRUTINY succeeds
        mockMvc.perform(post("/api/v1/applications/{id}/actions", savedApp.getApplicationReference())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Due soon"))
                .andExpect(jsonPath("$.currentStage").value("EVC scheduling"))
                .andExpect(jsonPath("$.ownerName").value("C. Verma (EVC Coordinator)"))
                .andExpect(jsonPath("$.timeline[0].title").value("Scrutiny Cleared · Forwarded to EVC scheduling"));

        // 7. Verify database transition records
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByApplicationIdOrderByCreatedAtDesc(savedApp.getId());
        assertThat(transitions).hasSize(5);
        assertThat(transitions.get(0).getAction()).isEqualTo("COMPLETE_SCRUTINY");
        assertThat(transitions.get(1).getAction()).isEqualTo("RESOLVE_DEFICIENCY");
        assertThat(transitions.get(2).getAction()).isEqualTo("RESOLVE_DEFICIENCY");
        assertThat(transitions.get(3).getAction()).isEqualTo("SUBMIT_CORRECTIONS");
        assertThat(transitions.get(4).getAction()).isEqualTo("RETURN_FOR_CORRECTION");
    }
}
