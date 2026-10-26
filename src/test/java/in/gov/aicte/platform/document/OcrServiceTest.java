package in.gov.aicte.platform.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrServiceTest {
    @TempDir Path tempDir;

    @Test
    void extractsSelectableTextFromPdfWithoutRequiringTesseract() throws Exception {
        Path pdf = tempDir.resolve("certificate.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(0))) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(40, 700);
                stream.showText("SAGE compliance certificate approval number 2026");
                stream.endText();
            }
            document.save(pdf.toFile());
        }

        OcrService.ExtractionResult result = new OcrService("tesseract", "eng", 10, 45)
                .extract(pdf, "certificate.pdf");

        assertEquals("TEXT_EXTRACTED", result.status());
        assertTrue(result.text().contains("SAGE compliance certificate approval number 2026"));
    }
}
