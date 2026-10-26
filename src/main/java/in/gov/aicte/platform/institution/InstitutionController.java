package in.gov.aicte.platform.institution;

import in.gov.aicte.platform.common.pagination.PageResponse;
import in.gov.aicte.platform.common.pagination.PaginationUtils;
import in.gov.aicte.platform.institution.dto.CreateInstitutionRequest;
import in.gov.aicte.platform.institution.dto.InstitutionResponse;
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
@RequestMapping("/api/v1/institutions")
@Tag(name = "Institutions", description = "Canonical institutional directory endpoints")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List institutions", description = "Retrieve a paginated list of canonical institutions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Institutions successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request")
    })
    public ResponseEntity<PageResponse<InstitutionResponse>> listInstitutions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Pageable pageable = PaginationUtils.createPageable(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(institutionService.listInstitutions(pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'REGIONAL_OFFICER', 'USER')")
    @Operation(summary = "Create institution", description = "Register a new canonical institution record")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Institution successfully created"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "409", description = "Duplicate institutional identifier conflict")
    })
    public ResponseEntity<InstitutionResponse> createInstitution(
            @Valid @RequestBody CreateInstitutionRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        InstitutionResponse response = institutionService.createInstitution(request, actorId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get institution details", description = "Retrieve a canonical institution by its primary ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Institution found and returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request"),
            @ApiResponse(responseCode = "404", description = "Institution not found")
    })
    public ResponseEntity<InstitutionResponse> getInstitutionById(@PathVariable Long id) {
        return ResponseEntity.ok(institutionService.getInstitutionById(id));
    }
}
