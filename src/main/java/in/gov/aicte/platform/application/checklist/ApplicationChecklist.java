package in.gov.aicte.platform.application.checklist;

import in.gov.aicte.platform.application.Application;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "application_checklists", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"application_id", "item_code"})
})
public class ApplicationChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "requirement_clause", length = 128)
    private String requirementClause;

    @Column(name = "is_verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "verified_by", length = 128)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ApplicationChecklist() {
    }

    public ApplicationChecklist(
            Application application,
            String itemCode,
            String label,
            String requirementClause,
            Boolean verified,
            String verifiedBy
    ) {
        this.application = application;
        this.itemCode = itemCode;
        this.label = label;
        this.requirementClause = requirementClause;
        this.verified = verified != null ? verified : false;
        this.verifiedBy = verifiedBy;
        if (Boolean.TRUE.equals(verified)) {
            this.verifiedAt = Instant.now();
        }
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

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRequirementClause() {
        return requirementClause;
    }

    public void setRequirementClause(String requirementClause) {
        this.requirementClause = requirementClause;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
        if (Boolean.TRUE.equals(verified) && this.verifiedAt == null) {
            this.verifiedAt = Instant.now();
        } else if (Boolean.FALSE.equals(verified)) {
            this.verifiedAt = null;
        }
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
