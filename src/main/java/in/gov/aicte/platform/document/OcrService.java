package in.gov.aicte.platform.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Extracts selectable PDF/text content and uses the installed Tesseract CLI for scanned files. */
@Service
public class OcrService {
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);
    private static final int MAX_TEXT_CHARS = 1_000_000;

    private final String tesseractCommand;
    private final String language;
    private final int maxPdfPages;
    private final long timeoutSeconds;

    public OcrService(Environment environment) {
        this(
                environment.getProperty("app.ocr.command", "tesseract"),
                environment.getProperty("app.ocr.language", "eng"),
                environment.getProperty("app.ocr.max-pdf-pages", Integer.class, 10),
                environment.getProperty("app.ocr.timeout-seconds", Long.class, 45L)
        );
    }

    OcrService(String tesseractCommand, String language, int maxPdfPages, long timeoutSeconds) {
        this.tesseractCommand = tesseractCommand;
        this.language = language;
        this.maxPdfPages = Math.max(1, maxPdfPages);
        this.timeoutSeconds = Math.max(5, timeoutSeconds);
    }

    public record ExtractionResult(String text, String status, String message) { }

    public ExtractionResult extract(Path source, String originalFileName) {
        if (source == null || !Files.isRegularFile(source) || !Files.isReadable(source)) {
            return new ExtractionResult("", "OCR_FAILED", "Stored file is missing or unreadable; OCR could not start.");
        }
        String name = originalFileName == null ? source.getFileName().toString() : originalFileName;
        String lower = name.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".txt") || lower.endsWith(".csv")) {
                String text = bounded(Files.readString(source, StandardCharsets.UTF_8));
                return new ExtractionResult(text, text.isBlank() ? "OCR_NO_TEXT" : "TEXT_EXTRACTED",
                        text.isBlank() ? "The uploaded text file is empty." : "Text read directly from the uploaded file.");
            }
            if (lower.endsWith(".pdf")) return extractPdf(source);
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                String text = bounded(runTesseract(source));
                return new ExtractionResult(text, text.isBlank() ? "OCR_NO_TEXT" : "OCR_COMPLETED",
                        text.isBlank() ? "Tesseract did not detect readable text in the image." : "Text recognized from the image by Tesseract OCR.");
            }
            return new ExtractionResult("", "OCR_UNSUPPORTED",
                    "The file was stored, but text extraction supports PDF, PNG, JPEG, TXT and CSV formats.");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            String status = isTesseractMissing(ex) ? "OCR_UNAVAILABLE" : "OCR_FAILED";
            String message = isTesseractMissing(ex)
                    ? "Tesseract is not installed or not on PATH. Install Tesseract and upload again to run OCR."
                    : "OCR/text extraction could not complete: " + safeMessage(ex);
            log.warn("OCR processing failed for {} (status={}): {}", name, status, ex.toString());
            return new ExtractionResult("", status, message);
        }
    }

    private ExtractionResult extractPdf(Path source) throws IOException, InterruptedException {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            String embeddedText = bounded(new PDFTextStripper().getText(document));
            if (embeddedText.replaceAll("\\s", "").length() >= 25) {
                return new ExtractionResult(embeddedText, "TEXT_EXTRACTED", "Selectable text extracted from the PDF; OCR was not needed.");
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder text = new StringBuilder();
            int pagesToProcess = Math.min(document.getNumberOfPages(), maxPdfPages);
            for (int page = 0; page < pagesToProcess; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 220, ImageType.RGB);
                Path tempImage = Files.createTempFile("sage-ocr-page-", ".png");
                try {
                    if (!ImageIO.write(image, "png", tempImage.toFile())) {
                        throw new IOException("Could not render PDF page as PNG for OCR.");
                    }
                    String pageText = runTesseract(tempImage);
                    if (!pageText.isBlank()) {
                        if (text.length() > 0) text.append("\n\n");
                        text.append("[Page ").append(page + 1).append("]\n").append(pageText.trim());
                    }
                    if (text.length() >= MAX_TEXT_CHARS) break;
                } finally {
                    Files.deleteIfExists(tempImage);
                }
            }
            String extracted = bounded(text.toString());
            if (extracted.isBlank() && !embeddedText.isBlank()) {
                return new ExtractionResult(embeddedText, "OCR_NO_TEXT", "OCR found no additional readable content; limited embedded PDF text is shown.");
            }
            boolean truncatedPages = document.getNumberOfPages() > pagesToProcess;
            String status = extracted.isBlank() ? "OCR_NO_TEXT" : (truncatedPages ? "OCR_PARTIAL" : "OCR_COMPLETED");
            String message = extracted.isBlank()
                    ? "No readable text was detected in the processed PDF pages."
                    : truncatedPages
                        ? "OCR completed for the first " + pagesToProcess + " pages. Increase OCR_MAX_PDF_PAGES to process more pages."
                        : "Scanned PDF pages processed with Tesseract OCR.";
            return new ExtractionResult(extracted, status, message);
        }
    }

    private String runTesseract(Path image) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("sage-tesseract-");
        Path outputBase = workDir.resolve("recognized");
        Path outputText = workDir.resolve("recognized.txt");
        Path processLog = workDir.resolve("tesseract.log");
        try {
            Process process = new ProcessBuilder(tesseractCommand, image.toString(), outputBase.toString(),
                    "-l", language, "--psm", "3")
                    .redirectErrorStream(true)
                    .redirectOutput(processLog.toFile())
                    .start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Tesseract timed out after " + timeoutSeconds + " seconds.");
            }
            if (process.exitValue() != 0) {
                String logText = Files.exists(processLog) ? Files.readString(processLog, StandardCharsets.UTF_8) : "";
                throw new IOException("Tesseract exited with code " + process.exitValue() + ": " + safeMessageText(logText));
            }
            return Files.exists(outputText) ? Files.readString(outputText, StandardCharsets.UTF_8) : "";
        } finally {
            try (Stream<Path> paths = Files.walk(workDir)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            } catch (IOException ignored) { }
        }
    }

    private static String bounded(String text) {
        if (text == null) return "";
        return text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
    }

    private static boolean isTesseractMissing(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (cause instanceof IOException && message != null
                    && (message.contains("Cannot run program") || message.contains("CreateProcess error=2"))) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private static String safeMessage(Exception ex) {
        return safeMessageText(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }

    private static String safeMessageText(String value) {
        if (value == null || value.isBlank()) return "No further details available.";
        String compact = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "…" : compact;
    }
}
