package in.gov.aicte.platform.document;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "document_versions")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentRecord document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "verification_status", nullable = false, length = 64)
    private String verificationStatus;

    @Column(name = "uploaded_by", nullable = false, length = 128)
    private String uploadedBy;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "ocr_status", nullable = false, length = 32)
    private String ocrStatus = "PENDING";

    @Column(name = "ocr_message", columnDefinition = "TEXT")
    private String ocrMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DocumentVersion() {
    }

    public DocumentVersion(
            DocumentRecord document,
            Integer versionNumber,
            String fileName,
            String storagePath,
            Long fileSizeBytes,
            String sha256Hash,
            String notes,
            String verificationStatus,
            String uploadedBy
    ) {
        this.document = document;
        this.versionNumber = versionNumber;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
        this.notes = notes;
        this.verificationStatus = verificationStatus;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public DocumentRecord getDocument() { return document; }
    public void setDocument(DocumentRecord document) { this.document = document; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public String getOcrStatus() { return ocrStatus; }
    public void setOcrStatus(String ocrStatus) { this.ocrStatus = ocrStatus; }
    public String getOcrMessage() { return ocrMessage; }
    public void setOcrMessage(String ocrMessage) { this.ocrMessage = ocrMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
