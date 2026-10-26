package in.gov.aicte.platform.application.submission;

import in.gov.aicte.platform.application.Application;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "application_submissions")
public class ApplicationSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "submission_reference", nullable = false, unique = true, length = 64)
    private String submissionReference;

    @Column(name = "submission_type", nullable = false, length = 64)
    private String submissionType; // INITIAL_DOSSIER, RECTIFICATION_PACKAGE

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "submitted_by_id", nullable = false, length = 128)
    private String submittedById;

    @Column(name = "submitted_by_name", length = 128)
    private String submittedByName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "SUBMITTED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ApplicationSubmission() {
    }

    public ApplicationSubmission(
            Application application,
            String submissionReference,
            String submissionType,
            Integer versionNumber,
            String submittedById,
            String submittedByName,
            String notes,
            String status
    ) {
        this.application = application;
        this.submissionReference = submissionReference;
        this.submissionType = submissionType;
        this.versionNumber = versionNumber != null ? versionNumber : 1;
        this.submittedById = submittedById;
        this.submittedByName = submittedByName;
        this.notes = notes;
        this.status = status != null ? status : "SUBMITTED";
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public String getSubmissionReference() {
        return submissionReference;
    }

    public void setSubmissionReference(String submissionReference) {
        this.submissionReference = submissionReference;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getSubmittedById() {
        return submittedById;
    }

    public void setSubmittedById(String submittedById) {
        this.submittedById = submittedById;
    }

    public String getSubmittedByName() {
        return submittedByName;
    }

    public void setSubmittedByName(String submittedByName) {
        this.submittedByName = submittedByName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
