package in.gov.aicte.platform.alert;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformAlertRepository extends JpaRepository<PlatformAlert, Long>, JpaSpecificationExecutor<PlatformAlert> {

    @EntityGraph(attributePaths = {"institution"})
    Optional<PlatformAlert> findByAlertReference(String alertReference);

    @Override
    @EntityGraph(attributePaths = {"institution"})
    Optional<PlatformAlert> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"institution"})
    List<PlatformAlert> findAll();

    @EntityGraph(attributePaths = {"institution"})
    List<PlatformAlert> findByInstitutionIdOrderByCreatedAtDesc(Long institutionId);
}
