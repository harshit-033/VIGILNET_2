package in.gov.aicte.platform.document;

import in.gov.aicte.platform.document.dto.DocumentResponse;
import in.gov.aicte.platform.document.dto.VerifyDocumentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Registry", description = "Statutory documentary evidence upload, SHA-256 verification, and renewal tracking")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List registered documents", description = "Retrieve list of documents with optional status and search filtering")
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(documentService.listDocuments(institutionId, applicationId, verificationStatus, q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get document by ID or Reference", description = "Retrieve single document details including complete version history")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable String id) {
        return ResponseEntity.ok(documentService.getDocumentByIdOrReference(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INSTITUTION_REPRESENTATIVE', 'REGIONAL_OFFICER', 'ADMINISTRATOR', 'USER')")
    @Operation(summary = "Upload statutory document", description = "Upload file, compute SHA-256 cryptographic digest, and register document")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "institutionId", required = false) Long institutionId,
            @RequestParam(value = "applicationId", required = false) Long applicationId,
            @RequestParam(value = "issueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value = "notes", required = false) String notes,
            Principal principal
    ) throws IOException {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(documentService.uploadDocument(
                file, documentType, category, institutionId, applicationId, issueDate, expiryDate, notes, actorId
        ));
    }

    @PostMapping(value = "/{id}/renew", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INSTITUTION_REPRESENTATIVE', 'ADMINISTRATOR', 'USER')")
    @Operation(summary = "Submit document renewal", description = "Upload new version of expiring statutory certificate")
    public ResponseEntity<DocumentResponse> submitRenewal(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "issueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value = "notes", required = false) String notes,
            Principal principal
    ) throws IOException {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(documentService.submitRenewal(id, file, issueDate, expiryDate, notes, actorId));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('SCRUTINY_OFFICER', 'COMPLIANCE_REVIEWER', 'REGIONAL_OFFICER', 'ADMINISTRATOR', 'USER')")
    @Operation(summary = "Verify document", description = "Officer verifies statutory authenticity of uploaded certificate")
    public ResponseEntity<DocumentResponse> verifyDocument(
            @PathVariable String id,
            @Valid @RequestBody VerifyDocumentRequest request,
            Principal principal
    ) {
        String actorId = (principal != null) ? principal.getName() : "SYSTEM";
        return ResponseEntity.ok(documentService.verifyDocument(id, request, actorId));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download document file", description = "Stream stored file bytes from durable local storage")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String id) {
        DocumentResponse doc = documentService.getDocumentByIdOrReference(id);
        Resource resource = documentService.loadDocumentResource(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(doc.mimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.fileName() + "\"")
                .body(resource);
    }
}
