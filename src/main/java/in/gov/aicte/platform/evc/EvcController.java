package in.gov.aicte.platform.evc;

import in.gov.aicte.platform.evc.dto.CompleteVisitRequest;
import in.gov.aicte.platform.evc.dto.EvcMetricsResponse;
import in.gov.aicte.platform.evc.dto.EvcResponse;
import in.gov.aicte.platform.evc.dto.RescheduleEvcRequest;
import in.gov.aicte.platform.evc.dto.ScheduleEvcRequest;
import in.gov.aicte.platform.evc.dto.SubmitEvcReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/evc")
@Tag(name = "EVC Operations", description = "Expert Visit Committee (EVC) scheduling, inspection visits, and report endpoints")
public class EvcController {

    private final EvcService evcService;

    public EvcController(EvcService evcService) {
        this.evcService = evcService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EVC_COORDINATOR', 'EVC_EXPERT', 'REGIONAL_OFFICER', 'SCRUTINY_OFFICER', 'SENIOR_MANAGEMENT', 'ADMINISTRATOR', 'AUDITOR', 'INSTITUTION_REPRESENTATIVE')")
    @Operation(summary = "List EVC visits", description = "Retrieve list of all EVC visits with optional status, region, or keyword filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visits successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request")
    })
    public ResponseEntity<List<EvcResponse>> listVisits(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(evcService.listVisits(status, region, q));
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('EVC_COORDINATOR', 'EVC_EXPERT', 'REGIONAL_OFFICER', 'SCRUTINY_OFFICER', 'SENIOR_MANAGEMENT', 'ADMINISTRATOR', 'AUDITOR')")
    @Operation(summary = "EVC dashboard metrics", description = "Calculate real-time KPIs for Total EVC Cases, Pending Scheduling, Upcoming, Completed, Reports Pending, and Overdue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metrics successfully calculated"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request")
    })
    public ResponseEntity<EvcMetricsResponse> getMetrics() {
        return ResponseEntity.ok(evcService.getMetrics());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EVC_COORDINATOR', 'EVC_EXPERT', 'REGIONAL_OFFICER', 'SCRUTINY_OFFICER', 'SENIOR_MANAGEMENT', 'ADMINISTRATOR', 'AUDITOR', 'INSTITUTION_REPRESENTATIVE')")
    @Operation(summary = "Get EVC visit by ID or Reference", description = "Retrieve single EVC visit details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visit found and returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "EVC visit not found")
    })
    public ResponseEntity<EvcResponse> getVisitById(@PathVariable String id) {
        return ResponseEntity.ok(evcService.getVisitByIdOrReference(id));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('EVC_COORDINATOR', 'ADMINISTRATOR')")
    @Operation(summary = "Schedule EVC visit", description = "Assign inspection date, coordinator, inspection mode, and committee panel members")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visit successfully scheduled"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid status transition"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "EVC visit not found")
    })
    public ResponseEntity<EvcResponse> scheduleVisit(
            @PathVariable String id,
            @Valid @RequestBody ScheduleEvcRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(evcService.scheduleVisit(id, request, actorId));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('EVC_COORDINATOR', 'EVC_EXPERT', 'ADMINISTRATOR')")
    @Operation(summary = "Mark EVC visit completed", description = "Record physical/virtual inspection conclusion and start report drafting clock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visit marked as completed"),
            @ApiResponse(responseCode = "400", description = "Visit is not in Scheduled status"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "EVC visit not found")
    })
    public ResponseEntity<EvcResponse> completeVisit(
            @PathVariable String id,
            @RequestBody(required = false) CompleteVisitRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        CompleteVisitRequest req = request != null ? request : new CompleteVisitRequest(null, null, null);
        return ResponseEntity.ok(evcService.completeVisit(id, req, actorId));
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('EVC_COORDINATOR', 'EVC_EXPERT', 'ADMINISTRATOR')")
    @Operation(summary = "Submit EVC inspection report", description = "Submit expert committee recommendation and advance application to Committee review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report submitted and application advanced"),
            @ApiResponse(responseCode = "400", description = "Mandatory report outcome missing or visit not completed"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "EVC visit not found")
    })
    public ResponseEntity<EvcResponse> submitReport(
            @PathVariable String id,
            @Valid @RequestBody SubmitEvcReportRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(evcService.submitReport(id, request, actorId));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('EVC_COORDINATOR', 'ADMINISTRATOR')")
    @Operation(summary = "Revert or reschedule EVC visit", description = "Reverts completed visit back to scheduled or adjusts visit date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visit successfully rescheduled or reverted"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "EVC visit not found")
    })
    public ResponseEntity<EvcResponse> rescheduleVisit(
            @PathVariable String id,
            @RequestBody(required = false) RescheduleEvcRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        RescheduleEvcRequest req = request != null ? request : new RescheduleEvcRequest(null, null, null);
        return ResponseEntity.ok(evcService.rescheduleVisit(id, req, actorId));
    }
}
