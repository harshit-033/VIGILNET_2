package in.gov.aicte.platform.alert.dto;

public record AlertMetricsResponse(
        long totalAlerts,
        long upcomingDeadlines,
        long overdueTasks,
        long evcReminders,
        long complianceAlerts,
        long escalations,
        long criticalAlerts
) {
}
