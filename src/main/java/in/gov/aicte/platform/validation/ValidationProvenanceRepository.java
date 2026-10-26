package in.gov.aicte.platform.validation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidationProvenanceRepository extends JpaRepository<ValidationProvenance, Long> {

    List<ValidationProvenance> findByApplicationIdOrderByEvaluatedAtDesc(Long applicationId);

    List<ValidationProvenance> findByApplicationIdAndIsValidTrue(Long applicationId);

    Optional<ValidationProvenance> findTopByApplicationIdAndRuleIdAndIsValidTrueOrderByEvaluatedAtDesc(
            Long applicationId, String ruleId);

    @Modifying
    @Query("UPDATE ValidationProvenance vp SET vp.isValid = false WHERE vp.application.id = :appId AND vp.ruleId = :ruleId")
    void invalidatePriorProvenance(@Param("appId") Long applicationId, @Param("ruleId") String ruleId);
}
