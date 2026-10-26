package in.gov.aicte.platform.document;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentRecord, Long>, JpaSpecificationExecutor<DocumentRecord> {

    @EntityGraph(attributePaths = {"institution", "application", "versions"})
    Optional<DocumentRecord> findByDocumentReference(String documentReference);

    @Override
    @EntityGraph(attributePaths = {"institution", "application", "versions"})
    Optional<DocumentRecord> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"institution", "application", "versions"})
    List<DocumentRecord> findAll();

    @EntityGraph(attributePaths = {"institution", "application", "versions"})
    List<DocumentRecord> findByInstitutionIdOrderByCreatedAtDesc(Long institutionId);

    @EntityGraph(attributePaths = {"institution", "application", "versions"})
    List<DocumentRecord> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    Optional<DocumentRecord> findBySha256Hash(String sha256Hash);
}
