package in.gov.aicte.platform.evc;

import in.gov.aicte.platform.application.Application;
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
@Table(name = "evc_visits")
public class EvcVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "evc_reference", nullable = false, unique = true, length = 64)
    private String evcReference;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "urgency", nullable = false, length = 32)
    private String urgency;

    @Column(name = "inspection_type", nullable = false, length = 64)
    private String inspectionType;

    @Column(name = "coordinator_name", nullable = false, length = 128)
    private String coordinatorName;

    @Column(name = "coordinator_id", nullable = false, length = 64)
    private String coordinatorId;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "experts_json", columnDefinition = "TEXT")
    private String expertsJson;

    @Column(name = "report_status", nullable = false, length = 128)
    private String reportStatus;

    @Column(name = "report_outcome", length = 64)
    private String reportOutcome;

    @Column(name = "report_notes", columnDefinition = "TEXT")
    private String reportNotes;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EvcVisit() {
    }

    public EvcVisit(Application application, String evcReference, String coordinatorName, String coordinatorId) {
        this.application = application;
        this.evcReference = evcReference;
        this.status = "Awaiting scheduling";
        this.urgency = "In progress";
        this.inspectionType = "Physical On-Site Verification";
        this.coordinatorName = coordinatorName != null ? coordinatorName : "C. Verma (EVC Operations)";
        this.coordinatorId = coordinatorId != null ? coordinatorId : "USR-EVC-01";
        this.reportStatus = "Not Applicable";
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = "Awaiting scheduling";
        }
        if (this.urgency == null) {
            this.urgency = "In progress";
        }
        if (this.inspectionType == null) {
            this.inspectionType = "Physical On-Site Verification";
        }
        if (this.reportStatus == null) {
            this.reportStatus = "Not Applicable";
        }
        if (this.coordinatorName == null) {
            this.coordinatorName = "C. Verma (EVC Operations)";
        }
        if (this.coordinatorId == null) {
            this.coordinatorId = "USR-EVC-01";
        }
    }

    @PreUpdate
    protected void onUpdate() {
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

    public String getEvcReference() {
        return evcReference;
    }

    public void setEvcReference(String evcReference) {
        this.evcReference = evcReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getInspectionType() {
        return inspectionType;
    }

    public void setInspectionType(String inspectionType) {
        this.inspectionType = inspectionType;
    }

    public String getCoordinatorName() {
        return coordinatorName;
    }

    public void setCoordinatorName(String coordinatorName) {
        this.coordinatorName = coordinatorName;
    }

    public String getCoordinatorId() {
        return coordinatorId;
    }

    public void setCoordinatorId(String coordinatorId) {
        this.coordinatorId = coordinatorId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getExpertsJson() {
        return expertsJson;
    }

    public void setExpertsJson(String expertsJson) {
        this.expertsJson = expertsJson;
    }

    public String getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(String reportStatus) {
        this.reportStatus = reportStatus;
    }

    public String getReportOutcome() {
        return reportOutcome;
    }

    public void setReportOutcome(String reportOutcome) {
        this.reportOutcome = reportOutcome;
    }

    public String getReportNotes() {
        return reportNotes;
    }

    public void setReportNotes(String reportNotes) {
        this.reportNotes = reportNotes;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvcVisit evcVisit = (EvcVisit) o;
        return Objects.equals(id, evcVisit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
