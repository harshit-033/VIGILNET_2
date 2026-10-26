package in.gov.aicte.platform;

import in.gov.aicte.platform.application.Application;
import in.gov.aicte.platform.application.ApplicationRepository;
import in.gov.aicte.platform.audit.AuditEvent;
import in.gov.aicte.platform.audit.AuditEventRepository;
import in.gov.aicte.platform.institution.Institution;
import in.gov.aicte.platform.institution.InstitutionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PostgresPersistenceIntegrationTest {

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify Flyway V1 through V7 migration history in PostgreSQL")
    void shouldVerifyFlywayMigrationStatus() {
        Integer countV1 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class
        );
        assertThat(countV1).isNotNull().isEqualTo(1);

        Integer countV7 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '7' AND success = true",
                Integer.class
        );
        assertThat(countV7).isNotNull().isEqualTo(1);

        Integer totalSuccess = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );
        assertThat(totalSuccess).isNotNull().isGreaterThanOrEqualTo(7);
    }

    @Test
    @DisplayName("Verify Institution, Application, and AuditEvent persistence with foreign keys and optimistic locking")
    void shouldPersistAndRetrieveEntitiesWithRelationsAndOptimisticLocking() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String instIdentifier = "INST-" + uniqueId;
        String appRef = "APP-" + uniqueId;

        // 1. Create and persist canonical Institution
        Institution institution = new Institution(instIdentifier, "Government Engineering College " + uniqueId);
        Institution savedInst = institutionRepository.saveAndFlush(institution);

        assertThat(savedInst.getId()).isNotNull();
        assertThat(savedInst.getCreatedAt()).isNotNull();

        Optional<Institution> fetchedInst = institutionRepository.findByInstitutionalIdentifier(instIdentifier);
        assertThat(fetchedInst).isPresent();
        assertThat(fetchedInst.get().getName()).contains("Government Engineering College");

        // 2. Create and persist Application linked to Institution
        Application application = new Application(
                savedInst,
                appRef,
                "EXTENSION_OF_APPROVAL",
                "2026-2027",
                "DRAFT"
        );
        Application savedApp = applicationRepository.saveAndFlush(application);

        assertThat(savedApp.getId()).isNotNull();
        assertThat(savedApp.getInstitution().getId()).isEqualTo(savedInst.getId());
        assertThat(savedApp.getVersion()).isEqualTo(0L);

        // 3. Update application to test @Version increment
        savedApp.setStatus("SUBMITTED");
        Application updatedApp = applicationRepository.saveAndFlush(savedApp);
        assertThat(updatedApp.getVersion()).isGreaterThanOrEqualTo(1L);

        // 4. Create and persist AuditEvent
        AuditEvent auditEvent = new AuditEvent(
                "APPLICATION_SUBMITTED",
                "USER-TEST-OFFICER",
                "APPLICATION",
                savedApp.getId().toString(),
                "{\"action\":\"submit\",\"reason\":\"Prototype test submission\"}"
        );
        AuditEvent savedAudit = auditEventRepository.saveAndFlush(auditEvent);

        assertThat(savedAudit.getId()).isNotNull();
        assertThat(savedAudit.getCreatedAt()).isNotNull();

        List<AuditEvent> auditHistory = auditEventRepository.findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
                "APPLICATION",
                savedApp.getId().toString()
        );
        assertThat(auditHistory).hasSize(1);
        assertThat(auditHistory.get(0).getEventType()).isEqualTo("APPLICATION_SUBMITTED");
    }
}
