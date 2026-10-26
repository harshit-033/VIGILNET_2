package in.gov.aicte.platform.alert.delivery;

import in.gov.aicte.platform.alert.PlatformAlert;

/**
 * Adapter interface for dispatching regulatory alerts across delivery channels
 * (SMS, Email, Push, Portal, or external government gateways).
 */
public interface NotificationDeliveryAdapter {

    DeliveryResult deliver(PlatformAlert alert);

    String getAdapterName();
}
