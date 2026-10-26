package in.gov.aicte.platform.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(String targetEntityType, String targetEntityId);

    List<AuditEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
