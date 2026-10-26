package in.gov.aicte.platform.alert;

import in.gov.aicte.platform.institution.Institution;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "platform_alerts")
public class PlatformAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_reference", nullable = false, unique = true, length = 64)
    private String alertReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(name = "type", nullable = false, length = 128)
    private String type;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity = "HIGH";

    @Column(name = "urgency", nullable = false, length = 32)
    private String urgency = "Due soon";

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "required_action", columnDefinition = "TEXT")
    private String requiredAction;

    @Column(name = "related_record_id", length = 64)
    private String relatedRecordId;

    @Column(name = "recipient", length = 128)
    private String recipient;

    @Column(name = "channel", length = 32)
    private String channel = "PORTAL";

    @Column(name = "delivery_status", length = 32)
    private String deliveryStatus = "Delivered";

    @Column(name = "delivery_attempts", nullable = false)
    private Integer deliveryAttempts = 1;

    @Column(name = "last_delivery_attempt_at")
    private Instant lastDeliveryAttemptAt = Instant.now();

    @Column(name = "last_delivery_error", columnDefinition = "TEXT")
    private String lastDeliveryError;

    @Column(name = "acknowledged", nullable = false)
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by", length = 128)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "escalated", nullable = false)
    private Boolean escalated = false;

    @Column(name = "escalated_to", length = 128)
    private String escalatedTo;

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PlatformAlert() {
    }

    public PlatformAlert(
            String alertReference,
            Institution institution,
            String type,
            String category,
            String severity,
            String urgency,
            String message,
            String requiredAction,
            String relatedRecordId,
            String recipient,
            LocalDate dueDate
    ) {
        this.alertReference = alertReference;
        this.institution = institution;
        this.type = type;
        this.category = category;
        this.severity = severity;
        this.urgency = urgency;
        this.message = message;
        this.requiredAction = requiredAction;
        this.relatedRecordId = relatedRecordId;
        this.recipient = recipient;
        this.dueDate = dueDate;
    }

    public Long getId() { return id; }
    public String getAlertReference() { return alertReference; }
    public void setAlertReference(String alertReference) { this.alertReference = alertReference; }
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRequiredAction() { return requiredAction; }
    public void setRequiredAction(String requiredAction) { this.requiredAction = requiredAction; }
    public String getRelatedRecordId() { return relatedRecordId; }
    public void setRelatedRecordId(String relatedRecordId) { this.relatedRecordId = relatedRecordId; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public Integer getDeliveryAttempts() { return deliveryAttempts; }
    public void setDeliveryAttempts(Integer deliveryAttempts) { this.deliveryAttempts = deliveryAttempts; }
    public Instant getLastDeliveryAttemptAt() { return lastDeliveryAttemptAt; }
    public void setLastDeliveryAttemptAt(Instant lastDeliveryAttemptAt) { this.lastDeliveryAttemptAt = lastDeliveryAttemptAt; }
    public String getLastDeliveryError() { return lastDeliveryError; }
    public void setLastDeliveryError(String lastDeliveryError) { this.lastDeliveryError = lastDeliveryError; }
    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public Boolean getEscalated() { return escalated; }
    public void setEscalated(Boolean escalated) { this.escalated = escalated; }
    public String getEscalatedTo() { return escalatedTo; }
    public void setEscalatedTo(String escalatedTo) { this.escalatedTo = escalatedTo; }
    public String getEscalationReason() { return escalationReason; }
    public void setEscalationReason(String escalationReason) { this.escalationReason = escalationReason; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Instant getCreatedAt() { return createdAt; }
}
