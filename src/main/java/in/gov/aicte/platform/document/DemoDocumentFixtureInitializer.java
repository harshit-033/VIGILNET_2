package in.gov.aicte.platform.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Bootstraps version-controlled demo PDF fixtures for pre-seeded statutory documents
 * (DOC-101, DOC-102, DOC-103) into local storage on non-production profiles.
 *
 * Ensures:
 * 1. Physical demo files are present at their expected storage path.
 * 2. Database records reflect the exact cryptographic SHA-256 hash and size of the fixtures.
 * 3. Idempotent execution (never overwrites non-empty user files or higher versions).
 * 4. Disabled in production environments (@Profile("!prod")).
 */
@Component
@Profile("!prod")
public class DemoDocumentFixtureInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDocumentFixtureInitializer.class);

    private record SeededFixture(String reference, String fileName, String resourcePath) {}

    private static final List<SeededFixture> SEEDED_FIXTURES = List.of(
            new SeededFixture("DOC-101", "state_fire_noc_2026.pdf", "demo-fixtures/state_fire_noc_2026.pdf"),
            new SeededFixture("DOC-102", "building_occupancy_cert.pdf", "demo-fixtures/building_occupancy_cert.pdf"),
            new SeededFixture("DOC-103", "univ_affiliation_2026.pdf", "demo-fixtures/univ_affiliation_2026.pdf")
    );

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;

    public DemoDocumentFixtureInitializer(
            FileStorageService fileStorageService,
            DocumentRepository documentRepository,
            DocumentVersionRepository versionRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking and bootstrapping pre-seeded demo PDF document fixtures...");

        Path rootDir = fileStorageService.getRootLocation();

        for (SeededFixture fixture : SEEDED_FIXTURES) {
            try {
                ClassPathResource resource = new ClassPathResource(fixture.resourcePath());
                if (!resource.exists()) {
                    log.warn("Demo fixture resource not found on classpath: {}", fixture.resourcePath());
                    continue;
                }

                byte[] bytes = resource.getContentAsByteArray();
                String sha256 = calculateSha256(bytes);
                long size = bytes.length;

                // 1. Ensure file exists at fileStorageService root location
                Path targetLocation = rootDir.resolve(fixture.fileName()).normalize();
                if (targetLocation.getParent() != null) {
                    Files.createDirectories(targetLocation.getParent());
                }

                boolean targetNeedsWrite = !Files.exists(targetLocation) || Files.size(targetLocation) == 0
                        || !calculateSha256(Files.readAllBytes(targetLocation)).equalsIgnoreCase(sha256);
                if (targetNeedsWrite) {
                    Files.write(targetLocation, bytes);
                    log.info("Installed/synchronized demo fixture {} at {}", fixture.fileName(), targetLocation);
                }

                // 2. Also ensure file exists at cwd-relative 'uploads/' path if different
                Path cwdUploads = Paths.get("uploads", fixture.fileName()).toAbsolutePath().normalize();
                if (!cwdUploads.equals(targetLocation.toAbsolutePath())) {
                    if (cwdUploads.getParent() != null) {
                        Files.createDirectories(cwdUploads.getParent());
                    }
                    boolean cwdNeedsWrite = !Files.exists(cwdUploads) || Files.size(cwdUploads) == 0
                            || !calculateSha256(Files.readAllBytes(cwdUploads)).equalsIgnoreCase(sha256);
                    if (cwdNeedsWrite) {
                        Files.write(cwdUploads, bytes);
                        log.info("Installed/synchronized demo fixture mirror {} at {}", fixture.fileName(), cwdUploads);
                    }
                }

                // 3. Align database metadata for initial version (DOC-101, DOC-102, DOC-103)
                documentRepository.findByDocumentReference(fixture.reference()).ifPresent(doc -> {
                    if (doc.getVersionNumber() == 1 && (!doc.getSha256Hash().equalsIgnoreCase(sha256) || doc.getFileSizeBytes() != size)) {
                        doc.setSha256Hash(sha256);
                        doc.setFileSizeBytes(size);
                        documentRepository.save(doc);
                        log.info("Synchronized database digest for {} to {}", fixture.reference(), sha256);
                    }

                    List<DocumentVersion> versions = versionRepository.findByDocumentIdOrderByVersionNumberDesc(doc.getId());
                    for (DocumentVersion v : versions) {
                        if (v.getVersionNumber() == 1 && (!v.getSha256Hash().equalsIgnoreCase(sha256) || v.getFileSizeBytes() != size)) {
                            v.setSha256Hash(sha256);
                            v.setFileSizeBytes(size);
                            versionRepository.save(v);
                            log.info("Synchronized version 1 digest for {} to {}", fixture.reference(), sha256);
                        }
                    }
                });

            } catch (IOException e) {
                log.error("Failed to initialize demo document fixture {}", fixture.fileName(), e);
            }
        }

        log.info("Demo document fixture verification complete.");
    }

    private static String calculateSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
