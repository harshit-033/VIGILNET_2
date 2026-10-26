package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.dto.CreateApplicationRequest;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.institution.dto.CreateInstitutionRequest;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InstitutionAndApplicationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    @DisplayName("Unauthenticated request to /api/v1/institutions returns 401 Unauthorized")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/institutions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "officer_sharma")
    @DisplayName("Create and retrieve institution successfully, verifying audit event")
    void shouldCreateAndRetrieveInstitution() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String identifier = "INST-" + unique;
        String name = "Apex National Polytechnic " + unique;

        CreateInstitutionRequest request = new CreateInstitutionRequest(identifier, name);

        MvcResult result = mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.institutionalIdentifier").value(identifier))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Long institutionId = objectMapper.readTree(responseJson).get("id").asLong();

        // Retrieve by ID
        mockMvc.perform(get("/api/v1/institutions/{id}", institutionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(institutionId))
                .andExpect(jsonPath("$.institutionalIdentifier").value(identifier))
                .andExpect(jsonPath("$.name").value(name));

        // Verify audit event persisted in the same transaction
        List<AuditEvent> audits = auditEventRepository.findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
                "INSTITUTION",
                institutionId.toString()
        );
        assertThat(audits).hasSize(1);
        AuditEvent audit = audits.get(0);
        assertThat(audit.getEventType()).isEqualTo("INSTITUTION_CREATED");
        assertThat(audit.getActorId()).isEqualTo("officer_sharma");
        assertThat(audit.getMetadataJson()).contains(identifier);
    }

    @Test
    @WithMockUser
    @DisplayName("Reject blank or invalid institution input with 400 ProblemDetail")
    void shouldRejectBlankOrInvalidInstitutionInput() throws Exception {
        CreateInstitutionRequest blankRequest = new CreateInstitutionRequest("   ", "   ");

        mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidFields.institutionalIdentifier").exists())
                .andExpect(jsonPath("$.invalidFields.name").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Reject duplicate institutional identifier with 409 Conflict")
    void shouldRejectDuplicateInstitutionalIdentifier() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String identifier = "INST-DUP-" + unique;

        CreateInstitutionRequest firstRequest = new CreateInstitutionRequest(identifier, "First Institute");
        mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        CreateInstitutionRequest secondRequest = new CreateInstitutionRequest(identifier, "Duplicate Institute");
        mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Institution already exists with identifier: " + identifier));
    }

    @Test
    @WithMockUser
    @DisplayName("Return 404 when institution or application is not found")
    void shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/institutions/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/v1/applications/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "scrutiny_lead")
    @DisplayName("Create and retrieve application linked to existing institution, verifying audit trail")
    void shouldCreateAndRetrieveApplicationLinkedToInstitution() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup institution
        CreateInstitutionRequest instReq = new CreateInstitutionRequest("INST-APP-" + unique, "State Engineering College");
        MvcResult instResult = mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(instReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long instId = objectMapper.readTree(instResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Create application linked to institution
        String appRef = "APP-" + unique;
        CreateApplicationRequest appReq = new CreateApplicationRequest(
                instId,
                appRef,
                "EXTENSION_OF_APPROVAL",
                "2026-2027"
        );

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.applicationReference").value(appRef))
                .andExpect(jsonPath("$.applicationType").value("EXTENSION_OF_APPROVAL"))
                .andExpect(jsonPath("$.academicYear").value("2026-2027"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.institution.id").value(instId))
                .andExpect(jsonPath("$.institution.institutionalIdentifier").value("INST-APP-" + unique))
                .andReturn();

        Long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. Retrieve by ID
        mockMvc.perform(get("/api/v1/applications/{id}", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appId))
                .andExpect(jsonPath("$.applicationReference").value(appRef))
                .andExpect(jsonPath("$.institution.name").value("State Engineering College"));

        // 4. Verify audit event
        List<AuditEvent> audits = auditEventRepository.findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
                "APPLICATION",
                appId.toString()
        );
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getEventType()).isEqualTo("APPLICATION_CREATED");
        assertThat(audits.get(0).getActorId()).isEqualTo("scrutiny_lead");
    }

    @Test
    @WithMockUser
    @DisplayName("Reject application with nonexistent institution ID with 404 ProblemDetail")
    void shouldRejectApplicationWithNonexistentInstitution() throws Exception {
        CreateApplicationRequest appReq = new CreateApplicationRequest(
                888888L,
                "APP-GHOST-1",
                "NEW_INSTITUTION",
                "2026-2027"
        );

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Institution not found with id: 888888"));
    }

    @Test
    @WithMockUser
    @DisplayName("Reject duplicate application reference with 409 Conflict")
    void shouldRejectDuplicateApplicationReference() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // Setup institution
        CreateInstitutionRequest instReq = new CreateInstitutionRequest("INST-DUPAPP-" + unique, "Institute A");
        MvcResult instResult = mockMvc.perform(post("/api/v1/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(instReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long instId = objectMapper.readTree(instResult.getResponse().getContentAsString()).get("id").asLong();

        String ref = "APP-DUP-" + unique;
        CreateApplicationRequest firstApp = new CreateApplicationRequest(instId, ref, "TYPE_A", "2026-2027");
        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstApp)))
                .andExpect(status().isCreated());

        CreateApplicationRequest secondApp = new CreateApplicationRequest(instId, ref, "TYPE_B", "2026-2027");
        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondApp)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("Application already exists with reference: " + ref));
    }

    @Test
    @WithMockUser
    @DisplayName("Paginate and filter applications by institutionId, status, and academicYear")
    void shouldPaginateAndFilterApplications() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // Create 2 institutions
        CreateInstitutionRequest instReq1 = new CreateInstitutionRequest("INST-F1-" + unique, "Filter Inst 1");
        MvcResult res1 = mockMvc.perform(post("/api/v1/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(instReq1))).andReturn();
        Long instId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        CreateInstitutionRequest instReq2 = new CreateInstitutionRequest("INST-F2-" + unique, "Filter Inst 2");
        MvcResult res2 = mockMvc.perform(post("/api/v1/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(instReq2))).andReturn();
        Long instId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        // Create applications
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest(instId1, "APP-F1-" + unique, "TYPE_A", "2026-2027"))));

        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest(instId2, "APP-F2-" + unique, "TYPE_B", "2027-2028"))));

        // Test filter by institutionId
        mockMvc.perform(get("/api/v1/applications")
                        .param("institutionId", instId1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].applicationReference").value("APP-F1-" + unique));

        // Test filter by academicYear
        mockMvc.perform(get("/api/v1/applications")
                        .param("academicYear", "2027-2028"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].academicYear").value("2027-2028"));

        // Test pagination structure
        mockMvc.perform(get("/api/v1/applications")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.first").value(true));
    }
}
