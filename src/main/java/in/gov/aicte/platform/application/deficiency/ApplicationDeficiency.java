package in.gov.aicte.platform.application.deficiency;

import in.gov.aicte.platform.application.Application;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "application_deficiencies")
public class ApplicationDeficiency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "deficiency_code", nullable = false, length = 64)
    private String deficiencyCode;

    @Column(name = "requirement_reference", length = 128)
    private String requirementReference;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "OPEN"; // OPEN, RECTIFIED_PENDING_REVIEW, RESOLVED, RE_ISSUED

    @Column(name = "institution_response", columnDefinition = "TEXT")
    private String institutionResponse;

    @Column(name = "resolution_remarks", columnDefinition = "TEXT")
    private String resolutionRemarks;

    @Column(name = "issued_by", length = 128)
    private String issuedBy;

    @Column(name = "resolved_by", length = 128)
    private String resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ApplicationDeficiency() {
    }

    public ApplicationDeficiency(
            Application application,
            String deficiencyCode,
            String requirementReference,
            String description,
            String status,
            String issuedBy
    ) {
        this.application = application;
        this.deficiencyCode = deficiencyCode;
        this.requirementReference = requirementReference;
        this.description = description;
        this.status = status != null ? status : "OPEN";
        this.issuedBy = issuedBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
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

    public String getDeficiencyCode() {
        return deficiencyCode;
    }

    public void setDeficiencyCode(String deficiencyCode) {
        this.deficiencyCode = deficiencyCode;
    }

    public String getRequirementReference() {
        return requirementReference;
    }

    public void setRequirementReference(String requirementReference) {
        this.requirementReference = requirementReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInstitutionResponse() {
        return institutionResponse;
    }

    public void setInstitutionResponse(String institutionResponse) {
        this.institutionResponse = institutionResponse;
    }

    public String getResolutionRemarks() {
        return resolutionRemarks;
    }

    public void setResolutionRemarks(String resolutionRemarks) {
        this.resolutionRemarks = resolutionRemarks;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
