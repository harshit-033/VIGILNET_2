package in.gov.aicte.platform.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByInstitutionalIdentifier(String institutionalIdentifier);

    boolean existsByInstitutionalIdentifier(String institutionalIdentifier);
}
