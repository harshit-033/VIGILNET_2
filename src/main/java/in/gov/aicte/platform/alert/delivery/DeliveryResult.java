package in.gov.aicte.platform.alert.delivery;

public record DeliveryResult(
        boolean success,
        String errorDetails,
        String externalReference
) {
    public static DeliveryResult success(String externalReference) {
        return new DeliveryResult(true, null, externalReference);
    }

    public static DeliveryResult failure(String errorDetails) {
        return new DeliveryResult(false, errorDetails, null);
    }
}
