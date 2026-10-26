package in.gov.aicte.platform.document;

import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import in.gov.aicte.platform.auth.DevTokenService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeededDocumentAndOcrIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private DevTokenService devTokenService;

    private String instToken;

    @BeforeEach
    void setUp() {
        instToken = "Bearer " + devTokenService.issueToken("principal@abctech.edu.in", "Institution Representative").access_token();
    }

    @Test
    @DisplayName("Task 1: Pre-seeded documents DOC-101, DOC-102, and DOC-103 download with exact byte parity and HTTP 200")
    void shouldSuccessfullyDownloadAllSeededDocuments() throws Exception {
        List<String> seededRefs = List.of("DOC-101", "DOC-102", "DOC-103");

        for (String docRef : seededRefs) {
            DocumentRecord doc = documentRepository.findByDocumentReference(docRef)
                    .orElseThrow(() -> new AssertionError("Seeded document record missing from DB: " + docRef));

            // 1. Verify physical path resolves and file exists
            Path storedPath = Paths.get(doc.getStoragePath());
            if (!Files.exists(storedPath)) {
                storedPath = fileStorageService.getRootLocation().resolve(storedPath.getFileName());
            }
            assertThat(Files.exists(storedPath))
                    .withFailMessage("Physical file missing for %s at %s", docRef, doc.getStoragePath())
                    .isTrue();
            assertThat(Files.isReadable(storedPath)).isTrue();

            // 2. Verify file is valid PDF
            try (PDDocument pdDoc = Loader.loadPDF(storedPath.toFile())) {
                assertThat(pdDoc.getNumberOfPages()).isGreaterThan(0);
            }

            byte[] onDiskBytes = Files.readAllBytes(storedPath);
            String onDiskSha256 = sha256(onDiskBytes);

            // 3. Authenticated download via API endpoint
            MvcResult result = mockMvc.perform(get("/api/v1/documents/" + docRef + "/download")
                            .header(HttpHeaders.AUTHORIZATION, instToken))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\""))
                    .andReturn();

            byte[] downloadedBytes = result.getResponse().getContentAsByteArray();
            String downloadedSha256 = sha256(downloadedBytes);

            // 4. Verify byte parity and SHA-256 hash match
            assertThat(downloadedBytes).isEqualTo(onDiskBytes);
            assertThat(downloadedSha256).isEqualTo(onDiskSha256);
            assertThat(downloadedSha256).isEqualToIgnoringCase(doc.getSha256Hash());
            assertThat((long) downloadedBytes.length).isEqualTo(doc.getFileSizeBytes());
        }
    }

    @Test
    @DisplayName("Task 1 Regression: Missing file download returns HTTP 404 rather than HTTP 500")
    void shouldReturn404WhenDocumentFileIsMissingOnDisk() throws Exception {
        Institution inst = institutionRepository.findByInstitutionalIdentifier("INST-1025").orElse(null);

        // Save a record pointing to a non-existent file
        DocumentRecord phantomDoc = new DocumentRecord(
                "DOC-PHANTOM-" + System.currentTimeMillis(),
                inst,
                null,
                "Phantom Certificate",
                "Testing",
                "missing_file.pdf",
                "uploads/definitely_non_existent_" + System.currentTimeMillis() + ".pdf",
                1024L,
                "application/pdf",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                null,
                null,
                "Valid",
                "Verified",
                "System",
                "Test missing file",
                "Tester"
        );
        DocumentRecord saved = documentRepository.save(phantomDoc);

        mockMvc.perform(get("/api/v1/documents/" + saved.getDocumentReference() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, instToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Task 2: Digital PDF text extraction pathway extracts selectable English text via PDFBox")
    void shouldExtractDigitalPdfSelectableText() throws Exception {
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("AICTE STATUTORY APPROVAL ORDER 2026 FOR TECHNICAL EDUCATION");
                stream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            pdfBytes = baos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "digital_order_2026.pdf", "application/pdf", pdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("documentType", "AICTE Approval Order")
                        .param("category", "Statutory Approval")
                        .param("institutionId", "INST-1025")
                        .header(HttpHeaders.AUTHORIZATION, instToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("TEXT_EXTRACTED");
        assertThat(body).contains("AICTE STATUTORY APPROVAL ORDER 2026 FOR TECHNICAL EDUCATION");
    }

    @Test
    @DisplayName("Task 2: Scanned document OCR pathway extracts English text using Tesseract")
    void shouldExtractEnglishTextFromScannedImageViaTesseract() throws Exception {
        // Render an image containing crisp English printed text
        int width = 700;
        int height = 180;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("GOVERNMENT OF INDIA AICTE 2026", 40, 70);
        g.drawString("MANDATORY COMPLIANCE VALIDATED", 40, 120);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "scanned_certificate_sample.png", "image/png", pngBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("documentType", "Scanned Statutory Certificate")
                        .param("category", "Compliance")
                        .param("institutionId", "INST-1025")
                        .header(HttpHeaders.AUTHORIZATION, instToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("OCR_COMPLETED");
        assertThat(body).containsIgnoringCase("GOVERNMENT OF INDIA");
        assertThat(body).containsIgnoringCase("AICTE 2026");
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(data));
    }
}
