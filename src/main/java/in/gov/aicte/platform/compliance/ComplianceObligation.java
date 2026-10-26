package in.gov.aicte.platform.compliance;

import in.gov.aicte.platform.institution.Institution;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "compliance_obligations")
public class ComplianceObligation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "obligation_reference", nullable = false, unique = true, length = 64)
    private String obligationReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "category", nullable = false, length = 128)
    private String category;

    @Column(name = "requirement", nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "aph_clause", length = 128)
    private String aphClause;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "urgency", nullable = false, length = 32)
    private String urgency;

    @Column(name = "evidence_file", length = 255)
    private String evidenceFile;

    @Column(name = "evidence_hash", length = 64)
    private String evidenceHash;

    @Column(name = "evidence_notes", columnDefinition = "TEXT")
    private String evidenceNotes;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "submitted_by", length = 128)
    private String submittedBy;

    @Column(name = "reviewer", length = 128)
    private String reviewer;

    @Column(name = "verification_outcome", length = 64)
    private String verificationOutcome;

    @Column(name = "verification_remarks", columnDefinition = "TEXT")
    private String verificationRemarks;

    @Column(name = "verified_date")
    private LocalDate verifiedDate;

    @Column(name = "verified_by", length = 128)
    private String verifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ComplianceObligation() {
    }

    public ComplianceObligation(
            String obligationReference,
            Institution institution,
            String category,
            String requirement,
            String aphClause,
            LocalDate dueDate,
            String status,
            String urgency,
            String reviewer
    ) {
        this.obligationReference = obligationReference;
        this.institution = institution;
        this.category = category;
        this.requirement = requirement;
        this.aphClause = aphClause;
        this.dueDate = dueDate;
        this.status = status;
        this.urgency = urgency;
        this.reviewer = reviewer;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getObligationReference() { return obligationReference; }
    public void setObligationReference(String obligationReference) { this.obligationReference = obligationReference; }
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }
    public String getAphClause() { return aphClause; }
    public void setAphClause(String aphClause) { this.aphClause = aphClause; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getEvidenceFile() { return evidenceFile; }
    public void setEvidenceFile(String evidenceFile) { this.evidenceFile = evidenceFile; }
    public String getEvidenceHash() { return evidenceHash; }
    public void setEvidenceHash(String evidenceHash) { this.evidenceHash = evidenceHash; }
    public String getEvidenceNotes() { return evidenceNotes; }
    public void setEvidenceNotes(String evidenceNotes) { this.evidenceNotes = evidenceNotes; }
    public LocalDate getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDate submittedDate) { this.submittedDate = submittedDate; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getVerificationOutcome() { return verificationOutcome; }
    public void setVerificationOutcome(String verificationOutcome) { this.verificationOutcome = verificationOutcome; }
    public String getVerificationRemarks() { return verificationRemarks; }
    public void setVerificationRemarks(String verificationRemarks) { this.verificationRemarks = verificationRemarks; }
    public LocalDate getVerifiedDate() { return verifiedDate; }
    public void setVerifiedDate(LocalDate verifiedDate) { this.verifiedDate = verifiedDate; }
    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
