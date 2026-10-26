package in.gov.aicte.platform.application.deficiency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationDeficiencyRepository extends JpaRepository<ApplicationDeficiency, Long> {

    List<ApplicationDeficiency> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);

    long countByApplicationIdAndStatusIn(Long applicationId, List<String> statuses);
}
