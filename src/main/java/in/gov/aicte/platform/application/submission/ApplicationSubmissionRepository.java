package in.gov.aicte.platform.application.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationSubmissionRepository extends JpaRepository<ApplicationSubmission, Long> {

    List<ApplicationSubmission> findByApplicationIdOrderByVersionNumberDesc(Long applicationId);

    Optional<ApplicationSubmission> findTopByApplicationIdOrderByVersionNumberDesc(Long applicationId);

    long countByApplicationId(Long applicationId);
}
