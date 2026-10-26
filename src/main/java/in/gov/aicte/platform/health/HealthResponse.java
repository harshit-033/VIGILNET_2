package in.gov.aicte.platform.health;

import java.time.Instant;

public record HealthResponse(
    String status,
    String application,
    Instant timestamp
) {}
