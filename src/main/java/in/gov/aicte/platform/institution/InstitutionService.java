package in.gov.aicte.platform.institution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.common.exception.DuplicateResourceException;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import in.gov.aicte.platform.common.pagination.PageResponse;
import in.gov.aicte.platform.institution.dto.CreateInstitutionRequest;
import in.gov.aicte.platform.institution.dto.InstitutionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InstitutionService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionService.class);

    private final InstitutionRepository institutionRepository;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    private final in.gov.aicte.platform.auth.SecurityUtils securityUtils;

    public InstitutionService(
            InstitutionRepository institutionRepository,
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper,
            in.gov.aicte.platform.auth.SecurityUtils securityUtils
    ) {
        this.institutionRepository = institutionRepository;
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public InstitutionResponse createInstitution(CreateInstitutionRequest request, String actorId) {
        String identifier = request.institutionalIdentifier().trim();
        String name = request.name().trim();

        if (institutionRepository.existsByInstitutionalIdentifier(identifier)) {
            throw new DuplicateResourceException("Institution already exists with identifier: " + identifier);
        }

        Institution institution = new Institution(identifier, name);
        Institution saved = institutionRepository.save(institution);

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "CREATE_INSTITUTION");
        metadata.put("institutionalIdentifier", saved.getInstitutionalIdentifier());
        metadata.put("name", saved.getName());

        String auditMetadata;
        try {
            auditMetadata = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit metadata for institution id={}", saved.getId(), e);
            auditMetadata = "{}";
        }

        AuditEvent auditEvent = new AuditEvent(
                "INSTITUTION_CREATED",
                effectiveActor,
                "INSTITUTION",
                saved.getId().toString(),
                auditMetadata
        );
        auditEventRepository.save(auditEvent);

        log.info("Institution created id={} identifier='{}' by actor='{}'", saved.getId(), saved.getInstitutionalIdentifier(), effectiveActor);
        return InstitutionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public InstitutionResponse getInstitutionById(Long id) {
        Institution inst = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + id));
        securityUtils.assertInstitutionAccess(inst);
        securityUtils.assertRegionAccess(inst.getRegion());
        return InstitutionResponse.from(inst);
    }

    @Transactional(readOnly = true)
    public PageResponse<InstitutionResponse> listInstitutions(Pageable pageable) {
        Page<Institution> page = institutionRepository.findAll(pageable);
        return PageResponse.from(page.map(InstitutionResponse::from));
    }
}
