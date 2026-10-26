package in.gov.aicte.platform.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.aicte.platform.alert.dto.AlertMetricsResponse;
import in.gov.aicte.platform.alert.dto.AlertResponse;
import in.gov.aicte.platform.alert.dto.EscalateAlertRequest;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.auth.SecurityUtils;
import in.gov.aicte.platform.auth.dto.DevUserDto;
import in.gov.aicte.platform.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final PlatformAlertRepository alertRepository;
    private final AuditEventRepository auditEventRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public AlertService(
            PlatformAlertRepository alertRepository,
            AuditEventRepository auditEventRepository,
            SecurityUtils securityUtils,
            ObjectMapper objectMapper
    ) {
        this.alertRepository = alertRepository;
        this.auditEventRepository = auditEventRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> listAlerts(Long institutionId, String category, String urgency, String severity, String q) {
        List<PlatformAlert> all = alertRepository.findAll();

        final String assignedInstId = securityUtils.hasRole(SecurityUtils.ROLE_INSTITUTION_REPRESENTATIVE)
                ? securityUtils.getCurrentUser().map(DevUserDto::institutionId).orElse(null)
                : null;

        final String assignedRegion = securityUtils.hasRole(SecurityUtils.ROLE_REGIONAL_OFFICER)
                ? securityUtils.getCurrentUser().map(DevUserDto::region).orElse(null)
                : null;

        LocalDate today = LocalDate.now();

        return all.stream()
                .filter(alert -> {
                    // Dynamic urgency update
                    if (alert.getDueDate() != null && !Boolean.TRUE.equals(alert.getAcknowledged())) {
                        if (alert.getDueDate().isBefore(today)) {
                            alert.setUrgency("Overdue");
                        } else if (alert.getDueDate().isBefore(today.plusDays(7))) {
                            alert.setUrgency("Due soon");
                        }
                    }

                    if (assignedInstId != null && !assignedInstId.isBlank()) {
                        String instIdent = alert.getInstitution() != null ? alert.getInstitution().getInstitutionalIdentifier() : null;
                        if (instIdent == null || !assignedInstId.equalsIgnoreCase(instIdent)) {
                            return false;
                        }
                    }

                    if (assignedRegion != null && !assignedRegion.isBlank()
                            && !"All India".equalsIgnoreCase(assignedRegion)
                            && !"Multi-Region".equalsIgnoreCase(assignedRegion)
                            && !"Platform-wide".equalsIgnoreCase(assignedRegion)) {
                        String reg = alert.getInstitution() != null ? alert.getInstitution().getRegion() : null;
                        if (reg == null || !assignedRegion.equalsIgnoreCase(reg)) {
                            return false;
                        }
                    }

                    if (institutionId != null) {
                        if (alert.getInstitution() == null || !institutionId.equals(alert.getInstitution().getId())) {
                            return false;
                        }
                    }

                    if (category != null && !category.isBlank() && !"All Alerts".equalsIgnoreCase(category.trim())) {
                        if (!category.trim().equalsIgnoreCase(alert.getCategory())) {
                            return false;
                        }
                    }

                    if (urgency != null && !urgency.isBlank() && !"ALL".equalsIgnoreCase(urgency.trim())) {
                        if (!urgency.trim().equalsIgnoreCase(alert.getUrgency())) {
                            return false;
                        }
                    }

                    if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity.trim())) {
                        if (!severity.trim().equalsIgnoreCase(alert.getSeverity())) {
                            return false;
                        }
                    }

                    if (q != null && !q.isBlank()) {
                        String term = q.trim().toLowerCase();
                        String ref = alert.getAlertReference() != null ? alert.getAlertReference().toLowerCase() : "";
                        String type = alert.getType() != null ? alert.getType().toLowerCase() : "";
                        String msg = alert.getMessage() != null ? alert.getMessage().toLowerCase() : "";
                        String relId = alert.getRelatedRecordId() != null ? alert.getRelatedRecordId().toLowerCase() : "";
                        String instName = alert.getInstitution() != null ? alert.getInstitution().getName().toLowerCase() : "";
                        return ref.contains(term) || type.contains(term) || msg.contains(term) || relId.contains(term) || instName.contains(term);
                    }

                    return true;
                })
                .map(AlertResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertByIdOrReference(String idOrReference) {
        PlatformAlert alert = findAlert(idOrReference);
        securityUtils.assertInstitutionAccess(alert.getInstitution());
        if (alert.getInstitution() != null) {
            securityUtils.assertRegionAccess(alert.getInstitution().getRegion());
        }
        return AlertResponse.from(alert);
    }

    @Transactional
    public AlertResponse acknowledgeAlert(String idOrReference, String actorId) {
        PlatformAlert alert = findAlert(idOrReference);
        securityUtils.assertInstitutionAccess(alert.getInstitution());

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(effectiveActor);
        alert.setAcknowledgedAt(Instant.now());

        PlatformAlert saved = alertRepository.save(alert);

        saveAuditEvent("ALERT_ACKNOWLEDGED", effectiveActor, "ALERT", saved.getAlertReference(), Map.of(
                "alertReference", saved.getAlertReference(),
                "acknowledgedBy", effectiveActor
        ));

        log.info("Alert acknowledged ref={} by actor={}", saved.getAlertReference(), effectiveActor);
        return AlertResponse.from(saved);
    }

    @Transactional
    public AlertResponse escalateAlert(String idOrReference, EscalateAlertRequest request, String actorId) {
        PlatformAlert alert = findAlert(idOrReference);
        if (alert.getInstitution() != null) {
            securityUtils.assertRegionAccess(alert.getInstitution().getRegion());
        }

        String effectiveActor = (actorId != null && !actorId.isBlank()) ? actorId : "SYSTEM";
        String actorDisplay = (request.actorName() != null && !request.actorName().isBlank()) ? request.actorName() : effectiveActor;

        alert.setEscalated(true);
        alert.setEscalatedTo(request.escalateTo() != null ? request.escalateTo().trim() : "Regional Officer / Senior Bureau Desk");
        alert.setEscalationReason(request.reason().trim());
        alert.setSeverity("CRITICAL");

        PlatformAlert saved = alertRepository.save(alert);

        saveAuditEvent("ALERT_ESCALATED", effectiveActor, "ALERT", saved.getAlertReference(), Map.of(
                "alertReference", saved.getAlertReference(),
                "escalatedTo", saved.getEscalatedTo(),
                "reason", saved.getEscalationReason(),
                "actorName", actorDisplay
        ));

        log.info("Alert escalated ref={} to={} by actor={}", saved.getAlertReference(), saved.getEscalatedTo(), actorDisplay);
        return AlertResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AlertMetricsResponse getMetrics() {
        List<PlatformAlert> alerts = alertRepository.findAll();
        LocalDate today = LocalDate.now();

        long total = alerts.size();
        long upcoming = alerts.stream()
                .filter(a -> "Upcoming Deadlines".equalsIgnoreCase(a.getCategory()) ||
                        (a.getDueDate() != null && !a.getDueDate().isBefore(today) && a.getDueDate().isBefore(today.plusDays(7))))
                .count();
        long overdue = alerts.stream()
                .filter(a -> "Overdue Tasks".equalsIgnoreCase(a.getCategory()) ||
                        (a.getDueDate() != null && a.getDueDate().isBefore(today)))
                .count();
        long evc = alerts.stream()
                .filter(a -> "EVC Reminders".equalsIgnoreCase(a.getCategory()) || a.getType().contains("EVC"))
                .count();
        long compliance = alerts.stream()
                .filter(a -> "Compliance Alerts".equalsIgnoreCase(a.getCategory()) || a.getType().contains("Compliance") || a.getType().contains("Obligation"))
                .count();
        long escalations = alerts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getEscalated()))
                .count();
        long critical = alerts.stream()
                .filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity()))
                .count();

        return new AlertMetricsResponse(total, upcoming, overdue, evc, compliance, escalations, critical);
    }

    private PlatformAlert findAlert(String idOrReference) {
        if (idOrReference == null || idOrReference.isBlank()) {
            throw new ResourceNotFoundException("Alert identifier cannot be empty.");
        }

        try {
            Long numericId = Long.parseLong(idOrReference.trim());
            return alertRepository.findById(numericId)
                    .orElseGet(() -> alertRepository.findByAlertReference(idOrReference.trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Alert not found with ID: " + idOrReference)));
        } catch (NumberFormatException ignored) {
        }

        return alertRepository.findByAlertReference(idOrReference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with reference: " + idOrReference));
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
