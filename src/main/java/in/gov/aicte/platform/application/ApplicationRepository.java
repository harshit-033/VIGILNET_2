package in.gov.aicte.platform.application;

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
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    @EntityGraph(attributePaths = {"institution"})
    Optional<Application> findByApplicationReference(String applicationReference);

    boolean existsByApplicationReference(String applicationReference);

    @EntityGraph(attributePaths = {"institution"})
    List<Application> findByInstitutionId(Long institutionId);

    @Override
    @EntityGraph(attributePaths = {"institution"})
    Optional<Application> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"institution"})
    Page<Application> findAll(Specification<Application> spec, Pageable pageable);
}
