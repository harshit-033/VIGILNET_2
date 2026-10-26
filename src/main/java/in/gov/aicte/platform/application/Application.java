package in.gov.aicte.platform.application;

import in.gov.aicte.platform.institution.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "application_reference", nullable = false, unique = true, length = 64)
    private String applicationReference;

    @Column(name = "application_type", nullable = false, length = 64)
    private String applicationType;

    @Column(name = "academic_year", nullable = false, length = 16)
    private String academicYear;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "current_stage", nullable = false, length = 64)
    private String currentStage;

    @Column(name = "owner_name", length = 128)
    private String ownerName;

    @Column(name = "owner_id", length = 64)
    private String ownerId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "deficiencies_json", columnDefinition = "TEXT")
    private String deficienciesJson;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Application() {
    }

    public Application(Institution institution, String applicationReference, String applicationType, String academicYear, String status) {
        this.institution = institution;
        this.applicationReference = applicationReference;
        this.applicationType = applicationType;
        this.academicYear = academicYear;
        this.status = status;
        this.currentStage = "Scrutiny";
    }

    public Application(Institution institution, String applicationReference, String applicationType, String academicYear,
                       String status, String currentStage, String ownerName, String ownerId, LocalDate dueDate, String observations) {
        this.institution = institution;
        this.applicationReference = applicationReference;
        this.applicationType = applicationType;
        this.academicYear = academicYear;
        this.status = status;
        this.currentStage = currentStage != null ? currentStage : "Scrutiny";
        this.ownerName = ownerName;
        this.ownerId = ownerId;
        this.dueDate = dueDate;
        this.observations = observations;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "DRAFT";
        }
        if (this.currentStage == null) {
            this.currentStage = "Scrutiny";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getDeficienciesJson() {
        return deficienciesJson;
    }

    public void setDeficienciesJson(String deficienciesJson) {
        this.deficienciesJson = deficienciesJson;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return Objects.equals(applicationReference, that.applicationReference);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(applicationReference);
    }
}
