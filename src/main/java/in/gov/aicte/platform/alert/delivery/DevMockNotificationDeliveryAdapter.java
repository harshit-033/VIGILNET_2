package in.gov.aicte.platform.alert.delivery;

import in.gov.aicte.platform.alert.PlatformAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Deterministic Development & Testing Notification Delivery Adapter.
 * Provides configurable simulation hooks for unit and integration testing
 * of success, transient outage, retry, and terminal failure pathways.
 */
@Component
public class DevMockNotificationDeliveryAdapter implements NotificationDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(DevMockNotificationDeliveryAdapter.class);

    private volatile boolean simulatePermanentFailure = false;
    private volatile int transientFailuresRemaining = 0;

    @Override
    public DeliveryResult deliver(PlatformAlert alert) {
        if (simulatePermanentFailure) {
            log.warn("DevMockNotificationDeliveryAdapter: Simulated terminal failure for alert {}", alert.getAlertReference());
            return DeliveryResult.failure("External notification gateway unreachable (HTTP 500 Connection Refused)");
        }

        if (transientFailuresRemaining > 0) {
            transientFailuresRemaining--;
            log.warn("DevMockNotificationDeliveryAdapter: Simulated transient failure (remaining: {}) for alert {}",
                    transientFailuresRemaining, alert.getAlertReference());
            return DeliveryResult.failure("Temporary gateway rate-limit / socket timeout (HTTP 503 Service Unavailable)");
        }

        String externalRef = "EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("DevMockNotificationDeliveryAdapter: Delivered alert {} via channel {} to recipient {} (externalRef: {})",
                alert.getAlertReference(), alert.getChannel(), alert.getRecipient(), externalRef);
        return DeliveryResult.success(externalRef);
    }

    @Override
    public String getAdapterName() {
        return "DevMockNotificationDeliveryAdapter";
    }

    public void setSimulatePermanentFailure(boolean simulatePermanentFailure) {
        this.simulatePermanentFailure = simulatePermanentFailure;
    }

    public void setTransientFailuresRemaining(int count) {
        this.transientFailuresRemaining = count;
    }

    public void reset() {
        this.simulatePermanentFailure = false;
        this.transientFailuresRemaining = 0;
    }
}
