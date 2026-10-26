package in.gov.aicte.platform.alert;

import in.gov.aicte.platform.alert.dto.AlertMetricsResponse;
import in.gov.aicte.platform.alert.dto.AlertResponse;
import in.gov.aicte.platform.alert.dto.EscalateAlertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts & Escalations", description = "Regulatory turnaround deadlines, EVC reminders, and multi-tier SLA escalation management")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List alerts", description = "Retrieve alerts with category, urgency, severity, and keyword filtering")
    public ResponseEntity<List<AlertResponse>> listAlerts(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(alertService.listAlerts(institutionId, category, urgency, severity, q));
    }

    @GetMapping("/metrics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Alert metrics", description = "Calculate real-time counts of overdue tasks, SLA breaches, and critical alerts")
    public ResponseEntity<AlertMetricsResponse> getMetrics() {
        return ResponseEntity.ok(alertService.getMetrics());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get alert by ID or Reference", description = "Retrieve single alert details")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable String id) {
        return ResponseEntity.ok(alertService.getAlertByIdOrReference(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('REGIONAL_OFFICER', 'SCRUTINY_OFFICER', 'COMPLIANCE_REVIEWER', 'EVC_COORDINATOR', 'SENIOR_MANAGEMENT', 'ADMINISTRATOR')")
    @Operation(summary = "Create regulatory alert", description = "Register a new regulatory task, deadline alert, or SLA reminder")
    public ResponseEntity<AlertResponse> createAlert(
            @Valid @RequestBody in.gov.aicte.platform.alert.dto.CreateAlertRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        AlertResponse response = alertService.createAlert(request, actorId);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('REGIONAL_OFFICER', 'ADMINISTRATOR', 'COMPLIANCE_REVIEWER')")
    @Operation(summary = "Retry alert delivery", description = "Trigger retry dispatch on a failed or pending alert")
    public ResponseEntity<AlertResponse> retryAlert(
            @PathVariable String id,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(alertService.retryAlert(id, actorId));
    }

    @GetMapping("/{id}/attempts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get alert delivery history", description = "Retrieve list of all delivery and retry attempts for this alert")
    public ResponseEntity<List<in.gov.aicte.platform.alert.dto.AlertDeliveryAttemptResponse>> getDeliveryHistory(@PathVariable String id) {
        return ResponseEntity.ok(alertService.getDeliveryHistory(id));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Acknowledge alert", description = "Mark alert as read and acknowledged by authorized recipient")
    public ResponseEntity<AlertResponse> acknowledgeAlert(
            @PathVariable String id,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, actorId));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('REGIONAL_OFFICER', 'SCRUTINY_OFFICER', 'COMPLIANCE_REVIEWER', 'EVC_COORDINATOR', 'SENIOR_MANAGEMENT', 'ADMINISTRATOR')")
    @Operation(summary = "Escalate alert", description = "Manually escalate breached regulatory SLA to higher bureaucratic tier")
    public ResponseEntity<AlertResponse> escalateAlert(
            @PathVariable String id,
            @Valid @RequestBody EscalateAlertRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(alertService.escalateAlert(id, request, actorId));
    }
}
