package in.gov.aicte.platform.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "docx", "xlsx", "csv", "txt"
    );

    private final Path rootLocation;

    public FileStorageService(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize storage directory at {}", this.rootLocation, e);
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public record StoredFile(
            String originalFileName,
            String storagePath,
            String sha256Hash,
            long sizeBytes,
            String mimeType
    ) {}

    public StoredFile store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "document_" + System.currentTimeMillis();
        }

        // Sanitize filename
        String cleanName = Paths.get(originalFilename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");

        // Validate allowed file types for statutory compliance and document registry
        String ext = "";
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < cleanName.length() - 1) {
            ext = cleanName.substring(dotIndex + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Unsupported file extension '." + ext + "'. Permitted statutory document formats are: PDF, PNG, JPG, JPEG, DOCX, XLSX, CSV, and TXT."
            );
        }

        String uniqueFileName = UUID.randomUUID() + "_" + cleanName;
        Path destinationFile = this.rootLocation.resolve(uniqueFileName).normalize();

        // Prevent directory traversal
        if (!destinationFile.getParent().equals(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside upload directory.");
        }

        // Compute SHA-256 hash while streaming to file
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        String sha256Hash = HexFormat.of().formatHex(digest.digest());

        // Copy file to disk
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        log.info("Stored durable file {} as {} (size={}, sha256={})", originalFilename, destinationFile, file.getSize(), sha256Hash);

        return new StoredFile(
                cleanName,
                destinationFile.toString(),
                sha256Hash,
                file.getSize(),
                mimeType
        );
    }

    public Resource loadAsResource(String storagePath) {
        try {
            Path file = Paths.get(storagePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + storagePath, e);
        }
    }
}
