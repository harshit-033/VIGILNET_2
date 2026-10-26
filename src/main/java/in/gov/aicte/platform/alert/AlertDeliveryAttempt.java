package in.gov.aicte.platform.alert;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alert_delivery_attempts")
public class AlertDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private PlatformAlert alert;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt = Instant.now();

    public AlertDeliveryAttempt() {
    }

    public AlertDeliveryAttempt(
            PlatformAlert alert,
            Integer attemptNumber,
            String channel,
            String status,
            String errorMessage,
            String externalReference
    ) {
        this.alert = alert;
        this.attemptNumber = attemptNumber;
        this.channel = channel;
        this.status = status;
        this.errorMessage = errorMessage;
        this.externalReference = externalReference;
    }

    public Long getId() { return id; }
    public PlatformAlert getAlert() { return alert; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public String getExternalReference() { return externalReference; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
