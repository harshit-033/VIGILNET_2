package in.gov.aicte.platform.application.checklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationChecklistRepository extends JpaRepository<ApplicationChecklist, Long> {

    List<ApplicationChecklist> findByApplicationIdOrderByItemCodeAsc(Long applicationId);

    Optional<ApplicationChecklist> findByApplicationIdAndItemCode(Long applicationId, String itemCode);

    long countByApplicationIdAndVerified(Long applicationId, Boolean verified);

    long countByApplicationId(Long applicationId);
}
