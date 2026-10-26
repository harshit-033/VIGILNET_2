package in.gov.aicte.platform.application;

import in.gov.aicte.platform.application.dto.ApplicationResponse;
import in.gov.aicte.platform.application.dto.CreateApplicationRequest;
import in.gov.aicte.platform.common.pagination.PageResponse;
import in.gov.aicte.platform.common.pagination.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Applications", description = "Application lifecycle management endpoints")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List applications", description = "Retrieve a paginated list of applications with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request")
    })
    public ResponseEntity<PageResponse<ApplicationResponse>> listApplications(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Pageable pageable = PaginationUtils.createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(applicationService.listApplications(institutionId, status, academicYear, region, stage, q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTITUTION_REPRESENTATIVE', 'ADMINISTRATOR')")
    @Operation(summary = "Create application", description = "Register a new application linked to a canonical institution")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application successfully created"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "Referenced institution does not exist"),
            @ApiResponse(responseCode = "409", description = "Duplicate application reference conflict")
    })
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        ApplicationResponse response = applicationService.createApplication(request, actorId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get application details", description = "Retrieve application details including linked institution information and workflow timeline")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found and returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable String id) {
        return ResponseEntity.ok(applicationService.getApplicationByIdOrReference(id));
    }

    @PostMapping("/{id}/actions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Execute workflow action", description = "Perform a state transition such as return for correction, resubmit, or complete scrutiny")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workflow action executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid action, missing mandatory fields, or invalid state prerequisite"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic locking conflict")
    })
    public ResponseEntity<ApplicationResponse> executeAction(
            @PathVariable String id,
            @Valid @RequestBody in.gov.aicte.platform.workflow.dto.WorkflowActionRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(applicationService.executeWorkflowAction(id, request, actorId));
    }
}
