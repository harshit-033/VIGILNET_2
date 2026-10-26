package in.gov.aicte.platform.workflow;

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
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "workflow_transitions")
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "from_stage", nullable = false, length = 64)
    private String fromStage;

    @Column(name = "to_stage", nullable = false, length = 64)
    private String toStage;

    @Column(name = "from_status", nullable = false, length = 32)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 32)
    private String toStatus;

    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;

    @Column(name = "actor_name", length = 128)
    private String actorName;

    @Column(name = "actor_role", length = 64)
    private String actorRole;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "deficiencies_json", columnDefinition = "TEXT")
    private String deficienciesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WorkflowTransition() {
    }

    public WorkflowTransition(Application application, String action, String fromStage, String toStage,
                              String fromStatus, String toStatus, String actorId, String actorName,
                              String actorRole, String reason, String observations, String deficienciesJson) {
        this.application = application;
        this.action = action;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.reason = reason;
        this.observations = observations;
        this.deficienciesJson = deficienciesJson;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public String getAction() {
        return action;
    }

    public String getFromStage() {
        return fromStage;
    }

    public String getToStage() {
        return toStage;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getReason() {
        return reason;
    }

    public String getObservations() {
        return observations;
    }

    public String getDeficienciesJson() {
        return deficienciesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
