package in.gov.aicte.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.alert.PlatformAlert;
import in.gov.aicte.platform.alert.PlatformAlertRepository;
import in.gov.aicte.platform.alert.dto.EscalateAlertRequest;
import in.gov.aicte.platform.auth.DevTokenService;
import in.gov.aicte.platform.compliance.ComplianceObligation;
import in.gov.aicte.platform.compliance.ComplianceObligationRepository;
import in.gov.aicte.platform.compliance.dto.SubmitComplianceEvidenceRequest;
import in.gov.aicte.platform.compliance.dto.VerifyComplianceRequest;
import in.gov.aicte.platform.document.DocumentRecord;
import in.gov.aicte.platform.document.DocumentRepository;
import in.gov.aicte.platform.document.dto.VerifyDocumentRequest;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComplianceDocumentAlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ComplianceObligationRepository complianceRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PlatformAlertRepository alertRepository;

    @Autowired
    private DevTokenService devTokenService;

    private String instToken;
    private String reviewerToken;
    private String adminToken;
    private Institution institution;

    @BeforeEach
    void setUp() {
        instToken = "Bearer " + devTokenService.issueToken("principal@abctech.edu.in", "Institution Representative").access_token();
        reviewerToken = "Bearer " + devTokenService.issueToken("compliance.rev@aicte-india.gov.in", "Compliance Reviewer").access_token();
        adminToken = "Bearer " + devTokenService.issueToken("admin@aicte-india.gov.in", "Administrator").access_token();

        institution = institutionRepository.findByInstitutionalIdentifier("INST-1025")
                .orElseGet(() -> institutionRepository.save(new Institution("INST-1025", "ABC Institute of Technology", "Northern Region")));
    }

    @Test
    @DisplayName("Phase 4: Compliance Lifecycle - Submit Evidence and Official Verification")
    void shouldExecuteComplianceLifecycle() throws Exception {
        String oblRef = "OBL-TEST-" + UUID.randomUUID().toString().substring(0, 6);
        ComplianceObligation obl = new ComplianceObligation(
                oblRef,
                institution,
                "Statutory Committees",
                "Constitution of Grievance Redressal Committee",
                "APH Norms 6.2",
                LocalDate.now().plusDays(20),
                "Pending submission",
                "Due soon",
                "D. Patel"
        );
        obl = complianceRepository.save(obl);

        // 1. Institution Rep submits evidence
        SubmitComplianceEvidenceRequest submitReq = new SubmitComplianceEvidenceRequest(
                "grc_committee_order_2026.pdf",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Committee constituted under chairmanship of senior professor with student representative.",
                "Dr. Ramesh Kumar",
                "Institution Representative"
        );

        mockMvc.perform(post("/api/v1/compliance/" + obl.getObligationReference() + "/submit")
                        .header("Authorization", instToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Evidence submitted"))
                .andExpect(jsonPath("$.evidenceFile").value("grc_committee_order_2026.pdf"));

        // 2. Reviewer verifies evidence
        VerifyComplianceRequest verifyReq = new VerifyComplianceRequest(
                "Verified compliant",
                "Committee composition verified compliant with AICTE notification requirements.",
                "D. Patel",
                "Compliance Reviewer"
        );

        mockMvc.perform(post("/api/v1/compliance/" + obl.getObligationReference() + "/verify")
                        .header("Authorization", reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Verified compliant"))
                .andExpect(jsonPath("$.verificationRemarks").value(verifyReq.remarks()));

        // 3. Check metrics endpoint
        mockMvc.perform(get("/api/v1/compliance/metrics")
                        .header("Authorization", reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalObligations").isNumber())
                .andExpect(jsonPath("$.verifiedCompliant").isNumber());
    }

    @Test
    @DisplayName("Phase 4: Document Registry - Upload with SHA-256 Digest, Download Bytes, and Versioned Renewal")
    void shouldUploadVerifyAndRenewDocumentWithSha256() throws Exception {
        byte[] originalContent = "OFFICIAL STATUTORY NOC CERTIFICATE CONTENT DATED 2026".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "municipal_fire_safety_noc.pdf",
                "application/pdf",
                originalContent
        );

        // 1. Upload initial document
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("documentType", "State Fire Service Safety NOC")
                        .param("category", "Safety & Building NOCs")
                        .param("institutionId", institution.getId().toString())
                        .param("issueDate", "2026-01-15")
                        .param("expiryDate", "2027-01-14")
                        .param("notes", "Mandatory fire safety NOC endorsement")
                        .header("Authorization", instToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentReference").exists())
                .andExpect(jsonPath("$.fileName").value("municipal_fire_safety_noc.pdf"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.fileHash").exists())
                .andReturn();

        String docRef = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("documentReference").asText();

        // 2. Download and verify stream matches original bytes exactly
        MvcResult downloadResult = mockMvc.perform(get("/api/v1/documents/" + docRef + "/download")
                        .header("Authorization", instToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"municipal_fire_safety_noc.pdf\""))
                .andReturn();

        assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo(originalContent);

        // 3. Submit renewal with new version
        byte[] renewalContent = "RENEWED OFFICIAL NOC EXTENSION 2027-2029".getBytes();
        MockMultipartFile renewalFile = new MockMultipartFile(
                "file",
                "fire_safety_noc_renewal.pdf",
                "application/pdf",
                renewalContent
        );

        mockMvc.perform(multipart("/api/v1/documents/" + docRef + "/renew")
                        .file(renewalFile)
                        .param("issueDate", "2027-01-15")
                        .param("expiryDate", "2029-01-14")
                        .param("notes", "Biennial renewal certificate")
                        .header("Authorization", instToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.fileName").value("fire_safety_noc_renewal.pdf"))
                .andExpect(jsonPath("$.verificationStatus").value("Renewal submitted"));

        // 4. Officer verifies document
        VerifyDocumentRequest verifyReq = new VerifyDocumentRequest(
                "Verified",
                "Renewal certificate authenticated with municipal portal.",
                "B. Singh",
                "Scrutiny Officer"
        );

        mockMvc.perform(post("/api/v1/documents/" + docRef + "/verify")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("Verified"));
    }

    @Test
    @DisplayName("Phase 4: Alerts & Escalations - Retrieval, Acknowledgment, and Manual Escalation")
    void shouldManageAlertsAndEscalations() throws Exception {
        String altRef = "ALT-TEST-" + UUID.randomUUID().toString().substring(0, 6);
        PlatformAlert alert = new PlatformAlert(
                altRef,
                institution,
                "EVC Inspection 48-Hour Report SLA",
                "EVC Reminders",
                "HIGH",
                "Due soon",
                "48-hour reporting clock running for inspection.",
                "Upload completed committee report",
                "EVC-1",
                "C. Verma",
                LocalDate.now().plusDays(2)
        );
        alert = alertRepository.save(alert);

        // 1. List alerts
        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", instToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 2. Acknowledge alert
        mockMvc.perform(post("/api/v1/alerts/" + alert.getAlertReference() + "/acknowledge")
                        .header("Authorization", instToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true));

        // 3. Escalate alert
        EscalateAlertRequest escalateReq = new EscalateAlertRequest(
                "SLA breached beyond allowable 48-hour buffer.",
                "Advisor Bureau Desk (Senior Management)",
                "C. Verma",
                "EVC Coordinator"
        );

        mockMvc.perform(post("/api/v1/alerts/" + alert.getAlertReference() + "/escalate")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(escalateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(true))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.escalatedTo").value(escalateReq.escalateTo()));

        // 4. Check alert metrics
        mockMvc.perform(get("/api/v1/alerts/metrics")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts").isNumber());
    }
}
