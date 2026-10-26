package in.gov.aicte.platform.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.auth.SecurityUtils;
import in.gov.aicte.platform.auth.dto.DevUserDto;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import in.gov.aicte.platform.document.dto.DocumentResponse;
import in.gov.aicte.platform.document.dto.VerifyDocumentRequest;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final InstitutionRepository institutionRepository;
    private final ApplicationRepository applicationRepository;
    private final FileStorageService fileStorageService;
    private final AuditEventRepository auditEventRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentVersionRepository versionRepository,
            InstitutionRepository institutionRepository,
            ApplicationRepository applicationRepository,
            FileStorageService fileStorageService,
            AuditEventRepository auditEventRepository,
            SecurityUtils securityUtils,
            ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.institutionRepository = institutionRepository;
        this.applicationRepository = applicationRepository;
        this.fileStorageService = fileStorageService;
        this.auditEventRepository = auditEventRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(Long institutionId, Long applicationId, String verificationStatus, String q) {
        List<DocumentRecord> all = documentRepository.findAll();

        final String assignedInstId = securityUtils.hasRole(SecurityUtils.ROLE_INSTITUTION_REPRESENTATIVE)
                ? securityUtils.getCurrentUser().map(DevUserDto::institutionId).orElse(null)
                : null;

        final String assignedRegion = securityUtils.hasRole(SecurityUtils.ROLE_REGIONAL_OFFICER)
                ? securityUtils.getCurrentUser().map(DevUserDto::region).orElse(null)
                : null;

        LocalDate today = LocalDate.now();

        return all.stream()
                .filter(doc -> {
                    // Update dynamic validity status
                    if (doc.getExpiryDate() != null) {
                        if (doc.getExpiryDate().isBefore(today)) {
                            doc.setValidityStatus("Overdue");
                        } else if (doc.getExpiryDate().isBefore(today.plusDays(30))) {
                            doc.setValidityStatus("Due soon");
                        } else {
                            doc.setValidityStatus("Valid");
                        }
                    }

                    if (assignedInstId != null && !assignedInstId.isBlank()) {
                        String instIdent = doc.getInstitution() != null ? doc.getInstitution().getInstitutionalIdentifier() : null;
                        if (instIdent == null || !assignedInstId.equalsIgnoreCase(instIdent)) {
                            return false;
                        }
                    }

                    if (assignedRegion != null && !assignedRegion.isBlank()
                            && !"All India".equalsIgnoreCase(assignedRegion)
                            && !"Multi-Region".equalsIgnoreCase(assignedRegion)
                            && !"Platform-wide".equalsIgnoreCase(assignedRegion)) {
                        String reg = doc.getInstitution() != null ? doc.getInstitution().getRegion() : null;
                        if (reg == null || !assignedRegion.equalsIgnoreCase(reg)) {
                            return false;
                        }
                    }

                    if (institutionId != null) {
                        if (doc.getInstitution() == null || !institutionId.equals(doc.getInstitution().getId())) {
                            return false;
                        }
                    }

                    if (applicationId != null) {
                        if (doc.getApplication() == null || !applicationId.equals(doc.getApplication().getId())) {
                            return false;
                        }
                    }

                    if (verificationStatus != null && !verificationStatus.isBlank() && !"ALL".equalsIgnoreCase(verificationStatus.trim())) {
                        if (!verificationStatus.trim().equalsIgnoreCase(doc.getVerificationStatus())) {
                            return false;
                        }
                    }

                    if (q != null && !q.isBlank()) {
                        String term = q.trim().toLowerCase();
                        String ref = doc.getDocumentReference() != null ? doc.getDocumentReference().toLowerCase() : "";
                        String type = doc.getDocumentType() != null ? doc.getDocumentType().toLowerCase() : "";
                        String cat = doc.getCategory() != null ? doc.getCategory().toLowerCase() : "";
                        String fname = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
                        String instName = doc.getInstitution() != null ? doc.getInstitution().getName().toLowerCase() : "";
                        return ref.contains(term) || type.contains(term) || cat.contains(term) || fname.contains(term) || instName.contains(term);
                    }

                    return true;
                })
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentByIdOrReference(String idOrReference) {
        DocumentRecord doc = findDocument(idOrReference);
        securityUtils.assertInstitutionAccess(doc.getInstitution());
        if (doc.getInstitution() != null) {
            securityUtils.assertRegionAccess(doc.getInstitution().getRegion());
        }
        return DocumentResponse.from(doc);
    }

    @Transactional
    public DocumentResponse uploadDocument(
            MultipartFile file,
            String documentType,
            String category,
            Long institutionId,
            Long applicationId,
            LocalDate issueDate,
            LocalDate expiryDate,
            String notes,
            String actorId
    ) throws IOException {
        Institution institution;
        if (institutionId != null) {
            institution = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + institutionId));
        } else {
            // Default to first institution or user's assigned institution
            String assignedInstId = securityUtils.getCurrentUser().map(DevUserDto::institutionId).orElse(null);
            if (assignedInstId != null) {
                institution = institutionRepository.findByInstitutionalIdentifier(assignedInstId)
                        .orElseThrow(() -> new ResourceNotFoundException("Institution not found with identifier: " + assignedInstId));
            } else {
                institution = institutionRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("No institutions configured."));
            }
        }

        securityUtils.assertInstitutionAccess(institution);

        Application application = null;
        if (applicationId != null) {
            application = applicationRepository.findById(applicationId).orElse(null);
        }

        FileStorageService.StoredFile stored = fileStorageService.store(file);

        String ref = "DOC-" + System.currentTimeMillis();
        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";

        DocumentRecord doc = new DocumentRecord(
                ref,
                institution,
                application,
                documentType != null ? documentType.trim() : "Statutory Document",
                category != null ? category.trim() : "General",
                stored.originalFileName(),
                stored.storagePath(),
                stored.sizeBytes(),
                stored.mimeType(),
                stored.sha256Hash(),
                issueDate,
                expiryDate,
                "Valid",
                "Verification pending",
                "B. Singh (Scrutiny Officer)",
                notes,
                effectiveActor
        );
        DocumentRecord saved = documentRepository.save(doc);

        // Record initial version 1
        DocumentVersion v1 = new DocumentVersion(
                saved,
                1,
                stored.originalFileName(),
                stored.storagePath(),
                stored.sizeBytes(),
                stored.sha256Hash(),
                notes != null ? notes : "Initial document upload",
                "Verification pending",
                effectiveActor
        );
        versionRepository.save(v1);

        saveAuditEvent("DOCUMENT_UPLOADED", effectiveActor, "DOCUMENT", saved.getDocumentReference(), Map.of(
                "documentReference", saved.getDocumentReference(),
                "fileName", saved.getFileName(),
                "sha256", saved.getSha256Hash(),
                "fileSize", saved.getFileSizeBytes()
        ));

        log.info("Document registered id={} ref={} hash={} actor={}", saved.getId(), saved.getDocumentReference(), saved.getSha256Hash(), effectiveActor);
        return DocumentResponse.from(saved);
    }

    @Transactional
    public DocumentResponse submitRenewal(
            String idOrReference,
            MultipartFile file,
            LocalDate issueDate,
            LocalDate expiryDate,
            String notes,
            String actorId
    ) throws IOException {
        DocumentRecord doc = findDocument(idOrReference);
        securityUtils.assertInstitutionAccess(doc.getInstitution());

        FileStorageService.StoredFile stored = fileStorageService.store(file);

        int nextVersion = doc.getVersionNumber() + 1;
        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";

        doc.setVersionNumber(nextVersion);
        doc.setFileName(stored.originalFileName());
        doc.setStoragePath(stored.storagePath());
        doc.setFileSizeBytes(stored.sizeBytes());
        doc.setMimeType(stored.mimeType());
        doc.setSha256Hash(stored.sha256Hash());
        if (issueDate != null) doc.setIssueDate(issueDate);
        if (expiryDate != null) doc.setExpiryDate(expiryDate);
        doc.setVerificationStatus("Renewal submitted");
        doc.setValidityStatus("Valid");
        if (notes != null) doc.setNotes(notes.trim());

        DocumentRecord saved = documentRepository.save(doc);

        // Append version
        DocumentVersion version = new DocumentVersion(
                saved,
                nextVersion,
                stored.originalFileName(),
                stored.storagePath(),
                stored.sizeBytes(),
                stored.sha256Hash(),
                notes != null ? notes.trim() : "Document renewal submitted",
                "Renewal submitted",
                effectiveActor
        );
        versionRepository.save(version);

        saveAuditEvent("DOCUMENT_RENEWAL_SUBMITTED", effectiveActor, "DOCUMENT", saved.getDocumentReference(), Map.of(
                "documentReference", saved.getDocumentReference(),
                "versionNumber", nextVersion,
                "fileName", saved.getFileName(),
                "sha256", saved.getSha256Hash()
        ));

        log.info("Document renewal registered ref={} version={} actor={}", saved.getDocumentReference(), nextVersion, effectiveActor);
        return DocumentResponse.from(saved);
    }

    @Transactional
    public DocumentResponse verifyDocument(String idOrReference, VerifyDocumentRequest request, String actorId) {
        DocumentRecord doc = findDocument(idOrReference);
        if (doc.getInstitution() != null) {
            securityUtils.assertRegionAccess(doc.getInstitution().getRegion());
        }

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        String actorDisplay = (request.actorName() != null && !request.actorName().isBlank()) ? request.actorName() : effectiveActor;

        String status = request.verificationStatus().trim();
        doc.setVerificationStatus(status);
        doc.setReviewer(actorDisplay);
        if (request.remarks() != null) {
            doc.setNotes((doc.getNotes() != null ? doc.getNotes() + "\n" : "") + "Verification remark: " + request.remarks().trim());
        }

        DocumentRecord saved = documentRepository.save(doc);

        saveAuditEvent("DOCUMENT_VERIFIED", effectiveActor, "DOCUMENT", saved.getDocumentReference(), Map.of(
                "documentReference", saved.getDocumentReference(),
                "verificationStatus", status,
                "verifier", actorDisplay
        ));

        log.info("Document verified ref={} status='{}' by verifier={}", saved.getDocumentReference(), status, actorDisplay);
        return DocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentResource(String idOrReference) {
        DocumentRecord doc = findDocument(idOrReference);
        securityUtils.assertInstitutionAccess(doc.getInstitution());
        if (doc.getInstitution() != null) {
            securityUtils.assertRegionAccess(doc.getInstitution().getRegion());
        }
        return fileStorageService.loadAsResource(doc.getStoragePath());
    }

    public DocumentRecord findDocument(String idOrReference) {
        if (idOrReference == null || idOrReference.isBlank()) {
            throw new ResourceNotFoundException("Document identifier cannot be empty.");
        }

        try {
            Long numericId = Long.parseLong(idOrReference.trim());
            return documentRepository.findById(numericId)
                    .orElseGet(() -> documentRepository.findByDocumentReference(idOrReference.trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + idOrReference)));
        } catch (NumberFormatException ignored) {
        }

        return documentRepository.findByDocumentReference(idOrReference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with reference: " + idOrReference));
    }

    private void saveAuditEvent(String eventType, String actorId, String targetType, String targetId, Map<String, Object> metadata) {
        String metaStr;
        try {
            metaStr = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            metaStr = "{}";
        }
        AuditEvent event = new AuditEvent(eventType, actorId, targetType, targetId, metaStr);
        auditEventRepository.save(event);
    }
}
