package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.alert.PlatformAlert;
import in.gov.aicte.platform.alert.PlatformAlertRepository;
import in.gov.aicte.platform.alert.delivery.DevMockNotificationDeliveryAdapter;
import in.gov.aicte.platform.alert.dto.CreateAlertRequest;
import in.gov.aicte.platform.auth.DevTokenService;
import in.gov.aicte.platform.compliance.ComplianceObligation;
import in.gov.aicte.platform.compliance.ComplianceObligationRepository;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SageComplianceEvidenceAndAlertRetryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DevTokenService devTokenService;

    @Autowired
    private DevMockNotificationDeliveryAdapter mockDeliveryAdapter;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private PlatformAlertRepository alertRepository;

    private String adminToken;
    private String instRepToken;
    private String foreignInstRepToken;
    private String regionalOfficerToken;

    private Institution college1;
    private Institution college2;
    private ComplianceObligation testObligation;

    @BeforeEach
    void setUp() {
        mockDeliveryAdapter.reset();

        adminToken = devTokenService.issueToken("admin@aicte-india.gov.in", "Administrator").access_token();
        instRepToken = devTokenService.issueToken("principal@abctech.edu.in", "Institution Representative").access_token();
        foreignInstRepToken = devTokenService.issueToken("principal@southpoly.edu.in", "Institution Representative").access_token();
        regionalOfficerToken = devTokenService.issueToken("ro.north@aicte-india.gov.in", "Regional Officer").access_token();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        college1 = institutionRepository.findByInstitutionalIdentifier("INST-1025")
                .orElseGet(() -> institutionRepository.save(new Institution("INST-1025", "Delhi Institute of Advanced Technology " + suffix, "Northern Region")));

        college2 = institutionRepository.findByInstitutionalIdentifier("INST-2048")
                .orElseGet(() -> institutionRepository.save(new Institution("INST-2048", "Southern Polytechnic College " + suffix, "Southern Region")));

        testObligation = new ComplianceObligation(
                "OBL-TEST-" + suffix,
                college1,
                "Safety & Infrastructure",
                "Periodic Fire Safety Certificate Renewal",
                "APH Appendix 6.1",
                LocalDate.now().plusDays(30),
                "Pending submission",
                "Normal",
                "Compliance Desk"
        );
        testObligation = obligationRepository.save(testObligation);
    }

    @AfterEach
    void tearDown() {
        mockDeliveryAdapter.reset();
    }

    @Test
    @DisplayName("SAGE Branding: Health endpoint returns 'sage-compliance-platform' application name")
    void testSageBrandingHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("sage-compliance-platform"));
    }

    @Test
    @DisplayName("Compliance Evidence: Upload real PDF file, store digest, and stream exact binary download")
    void testComplianceEvidenceUploadAndDownload() throws Exception {
        byte[] pdfContent = "%PDF-1.4 Mock Statutory Fire Safety Affidavit Content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fire_safety_affidavit.pdf",
                "application/pdf",
                pdfContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/compliance/" + testObligation.getObligationReference() + "/evidence")
                        .file(file)
                        .param("notes", "Uploaded certified copy of 2026 fire NOC")
                        .param("actorName", "Principal Dr. Kumar")
                        .header("Authorization", "Bearer " + instRepToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Evidence submitted"))
                .andExpect(jsonPath("$.evidenceFile").value("fire_safety_affidavit.pdf"))
                .andExpect(jsonPath("$.evidenceHash").isNotEmpty())
                .andExpect(jsonPath("$.storagePath").isNotEmpty())
                .andReturn();

        // Download and verify exact binary bytes
        MvcResult downloadResult = mockMvc.perform(get("/api/v1/compliance/" + testObligation.getObligationReference() + "/evidence/download")
                        .header("Authorization", "Bearer " + instRepToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo(pdfContent);
    }

    @Test
    @DisplayName("Compliance Evidence: Reject unsupported file type (.exe)")
    void testRejectUnsupportedFileType() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "file",
                "malicious_payload.exe",
                "application/x-msdownload",
                "MZ_EXECUTABLE_HEADER".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/compliance/" + testObligation.getObligationReference() + "/evidence")
                        .file(badFile)
                        .param("notes", "Attempting executable upload")
                        .header("Authorization", "Bearer " + instRepToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tenant Isolation: Foreign institution representative cannot upload evidence to another institution's obligation")
    void testForeignInstitutionCannotUploadEvidence() throws Exception {
        byte[] pdfContent = "%PDF-1.4 Foreign College Test".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foreign_proof.pdf",
                "application/pdf",
                pdfContent
        );

        mockMvc.perform(multipart("/api/v1/compliance/" + testObligation.getObligationReference() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + foreignInstRepToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Alerts & Delivery: Create alert, observe initial delivery attempt in audit history")
    void testAlertCreationAndDeliveryAttempt() throws Exception {
        CreateAlertRequest request = new CreateAlertRequest(
                college1.getId(),
                "Statutory NOC SLA Reminder",
                "Upcoming Deadlines",
                "HIGH",
                "Due soon",
                "Building occupancy certificate renewal is pending within 15 days.",
                "Upload renewed certificate",
                "OBL-101",
                "Principal Dr. Kumar",
                "EMAIL",
                LocalDate.now().plusDays(15)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/alerts")
                        .header("Authorization", "Bearer " + regionalOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryStatus").value("Delivered"))
                .andExpect(jsonPath("$.deliveryAttempts").value(1))
                .andReturn();

        String alertRef = objectMapper.readTree(result.getResponse().getContentAsString()).get("alertReference").asText();

        // Inspect delivery history endpoint
        mockMvc.perform(get("/api/v1/alerts/" + alertRef + "/attempts")
                        .header("Authorization", "Bearer " + regionalOfficerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attemptNumber").value(1))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].channel").value("EMAIL"));
    }

    @Test
    @DisplayName("Alert Delivery Retry: Transient failure triggers 'Pending Retry', subsequent retry succeeds and marks 'Delivered'")
    void testAlertTransientFailureAndSuccessfulRetry() throws Exception {
        // Set simulated transient failure for 1 attempt
        mockDeliveryAdapter.setTransientFailuresRemaining(1);

        CreateAlertRequest request = new CreateAlertRequest(
                college1.getId(),
                "Transient Outage Test Alert",
                "Upcoming Deadlines",
                "HIGH",
                "Due soon",
                "Testing transient failure simulation",
                null, null, "admin@aicte.gov.in", "PORTAL", LocalDate.now().plusDays(5)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/alerts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryStatus").value("Pending Retry"))
                .andExpect(jsonPath("$.deliveryAttempts").value(1))
                .andReturn();

        String alertRef = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("alertReference").asText();

        // Second attempt: Manual / scheduled retry should succeed
        mockMvc.perform(post("/api/v1/alerts/" + alertRef + "/retry")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("Delivered"))
                .andExpect(jsonPath("$.deliveryAttempts").value(2));

        // Verify history shows attempt 1 FAILED, attempt 2 SUCCESS
        mockMvc.perform(get("/api/v1/alerts/" + alertRef + "/attempts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[1].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Alert Delivery Retry: Permanent gateway failure marks alert as 'Failed' after reaching attempt limit")
    void testAlertTerminalFailureAfterMaxAttempts() throws Exception {
        mockDeliveryAdapter.setSimulatePermanentFailure(true);

        CreateAlertRequest request = new CreateAlertRequest(
                college1.getId(),
                "Terminal Outage Test Alert",
                "Overdue Tasks",
                "CRITICAL",
                "Overdue",
                "Simulating permanent external gateway outage",
                null, null, "ro.north@aicte-india.gov.in", "SMS", LocalDate.now().minusDays(1)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/alerts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryStatus").value("Pending Retry"))
                .andExpect(jsonPath("$.deliveryAttempts").value(1))
                .andReturn();

        String alertRef = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("alertReference").asText();

        // Attempt 2
        mockMvc.perform(post("/api/v1/alerts/" + alertRef + "/retry")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("Pending Retry"))
                .andExpect(jsonPath("$.deliveryAttempts").value(2));

        // Attempt 3: Terminal failure limit reached
        mockMvc.perform(post("/api/v1/alerts/" + alertRef + "/retry")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("Failed"))
                .andExpect(jsonPath("$.deliveryAttempts").value(3));
    }
}
