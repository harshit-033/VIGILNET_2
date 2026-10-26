package in.gov.aicte.platform.compliance;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceObligationRepository extends JpaRepository<ComplianceObligation, Long>, JpaSpecificationExecutor<ComplianceObligation> {

    @EntityGraph(attributePaths = {"institution"})
    Optional<ComplianceObligation> findByObligationReference(String obligationReference);

    @Override
    @EntityGraph(attributePaths = {"institution"})
    Optional<ComplianceObligation> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"institution"})
    List<ComplianceObligation> findAll();

    @EntityGraph(attributePaths = {"institution"})
    List<ComplianceObligation> findByInstitutionIdOrderByDueDateAsc(Long institutionId);
}
