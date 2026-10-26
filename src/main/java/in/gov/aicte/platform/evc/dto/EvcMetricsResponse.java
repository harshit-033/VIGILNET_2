package in.gov.aicte.platform.evc.dto;

public record EvcMetricsResponse(
        long totalCases,
        long pendingScheduling,
        long upcomingVisits,
        long completedVisits,
        long reportsPending,
        long overdueTasks
) {
}
