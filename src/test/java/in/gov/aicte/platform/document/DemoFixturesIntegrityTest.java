package in.gov.aicte.platform.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class DemoFixturesIntegrityTest {

    private static final String[] FIXTURE_FILES = {
            "state_fire_noc_2026.pdf",
            "building_occupancy_cert.pdf",
            "univ_affiliation_2026.pdf"
    };

    @Test
    void shouldVerifyDemoPdfFixturesAreValidAndClearlyLabelled() throws IOException {
        Path fixturesDir = Paths.get("src/main/resources/demo-fixtures");
        assertThat(fixturesDir).exists().isDirectory();

        for (String fileName : FIXTURE_FILES) {
            Path fixturePath = fixturesDir.resolve(fileName);
            assertThat(fixturePath).exists().isRegularFile();
            assertThat(Files.size(fixturePath)).isGreaterThan(100);

            try (PDDocument doc = Loader.loadPDF(fixturePath.toFile())) {
                assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
                String extractedText = new PDFTextStripper().getText(doc);
                assertThat(extractedText)
                        .contains("DEMO SAMPLE — NOT VALID FOR OFFICIAL USE")
                        .contains("ABC Institute of Technology (INST-1025)");
            }
        }
    }

    @Test
    void shouldVerifyDemoFixturesAreAvailableOnClasspath() throws IOException {
        for (String fileName : FIXTURE_FILES) {
            String resourcePath = "demo-fixtures/" + fileName;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                assertThat(is)
                        .withFailMessage("Resource " + resourcePath + " should be available on classpath")
                        .isNotNull();
            }
        }
    }
}
