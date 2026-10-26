package in.gov.aicte.platform.evc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvcVisitRepository extends JpaRepository<EvcVisit, Long>, JpaSpecificationExecutor<EvcVisit> {

    @EntityGraph(attributePaths = {"application", "application.institution"})
    Optional<EvcVisit> findByEvcReference(String evcReference);

    @EntityGraph(attributePaths = {"application", "application.institution"})
    Optional<EvcVisit> findByApplicationId(Long applicationId);

    @EntityGraph(attributePaths = {"application", "application.institution"})
    Optional<EvcVisit> findByApplicationApplicationReference(String applicationReference);

    @Override
    @EntityGraph(attributePaths = {"application", "application.institution"})
    Optional<EvcVisit> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"application", "application.institution"})
    List<EvcVisit> findAll();

    @Override
    @EntityGraph(attributePaths = {"application", "application.institution"})
    Page<EvcVisit> findAll(Specification<EvcVisit> spec, Pageable pageable);

    boolean existsByApplicationId(Long applicationId);

    boolean existsByEvcReference(String evcReference);
}
