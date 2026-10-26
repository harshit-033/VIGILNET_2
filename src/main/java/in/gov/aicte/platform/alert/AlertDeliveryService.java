package in.gov.aicte.platform.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.alert.delivery.DeliveryResult;
import in.gov.aicte.platform.alert.delivery.NotificationDeliveryAdapter;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AlertDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(AlertDeliveryService.class);
    private static final int MAX_DELIVERY_ATTEMPTS = 3;

    private final PlatformAlertRepository alertRepository;
    private final AlertDeliveryAttemptRepository attemptRepository;
    private final NotificationDeliveryAdapter deliveryAdapter;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AlertDeliveryService(
            PlatformAlertRepository alertRepository,
            AlertDeliveryAttemptRepository attemptRepository,
            NotificationDeliveryAdapter deliveryAdapter,
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper
    ) {
        this.alertRepository = alertRepository;
        this.attemptRepository = attemptRepository;
        this.deliveryAdapter = deliveryAdapter;
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlatformAlert sendOrRetry(PlatformAlert alert, String actorId) {
        int attemptNum = (alert.getDeliveryAttempts() != null ? alert.getDeliveryAttempts() : 0) + 1;
        alert.setDeliveryAttempts(attemptNum);
        alert.setLastDeliveryAttemptAt(Instant.now());

        DeliveryResult result = deliveryAdapter.deliver(alert);

        AlertDeliveryAttempt attempt;
        if (result.success()) {
            alert.setDeliveryStatus("Delivered");
            alert.setLastDeliveryError(null);
            attempt = new AlertDeliveryAttempt(
                    alert,
                    attemptNum,
                    alert.getChannel() != null ? alert.getChannel() : "PORTAL",
                    "SUCCESS",
                    null,
                    result.externalReference()
            );
        } else {
            alert.setLastDeliveryError(result.errorDetails());
            if (attemptNum >= MAX_DELIVERY_ATTEMPTS) {
                alert.setDeliveryStatus("Failed");
                log.warn("Alert {} reached max delivery attempts ({}) - marked as Failed",
                        alert.getAlertReference(), MAX_DELIVERY_ATTEMPTS);
            } else {
                alert.setDeliveryStatus("Pending Retry");
                log.info("Alert {} delivery failed on attempt {} - marked as Pending Retry",
                        alert.getAlertReference(), attemptNum);
            }
            attempt = new AlertDeliveryAttempt(
                    alert,
                    attemptNum,
                    alert.getChannel() != null ? alert.getChannel() : "PORTAL",
                    "FAILED",
                    result.errorDetails(),
                    null
            );
        }

        PlatformAlert saved = alertRepository.save(alert);
        attemptRepository.save(attempt);

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM_NOTIFICATION_DISPATCHER";
        saveAuditEvent("ALERT_DELIVERY_ATTEMPTED", effectiveActor, "ALERT", saved.getAlertReference(), Map.of(
                "alertReference", saved.getAlertReference(),
                "attemptNumber", attemptNum,
                "status", attempt.getStatus(),
                "adapter", deliveryAdapter.getAdapterName(),
                "deliveryStatus", saved.getDeliveryStatus()
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AlertDeliveryAttempt> getDeliveryHistory(Long alertId) {
        return attemptRepository.findByAlertIdOrderByAttemptedAtAsc(alertId);
    }

    @Transactional
    public int retryPendingAlerts(String actorId) {
        List<PlatformAlert> pending = alertRepository.findAll().stream()
                .filter(a -> "Pending Retry".equalsIgnoreCase(a.getDeliveryStatus())
                        && (a.getDeliveryAttempts() == null || a.getDeliveryAttempts() < MAX_DELIVERY_ATTEMPTS))
                .toList();

        int processed = 0;
        for (PlatformAlert alert : pending) {
            sendOrRetry(alert, actorId);
            processed++;
        }
        return processed;
    }

    private void saveAuditEvent(String eventType, String actorId, String targetType, String targetId, Map<String, Object> metadata) {
        String metaStr;
        try {
            metaStr = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            metaStr = "{}";
        }
        AuditEvent event = new AuditEvent(eventType, actorId, targetType, targetId, metaStr);
        auditEventRepository.save(event);
    }
}
