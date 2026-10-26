package in.gov.aicte.platform.application;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ApplicationSpecifications {

    private ApplicationSpecifications() {
    }

    public static Specification<Application> withFilters(Long institutionId, String status, String academicYear) {
        return withFilters(institutionId, status, academicYear, null, null, null);
    }

    public static Specification<Application> withFilters(
            Long institutionId,
            String status,
            String academicYear,
            String region,
            String stage,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (institutionId != null) {
                predicates.add(cb.equal(root.get("institution").get("id"), institutionId));
            }
            if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (academicYear != null && !academicYear.isBlank() && !"All".equalsIgnoreCase(academicYear.trim())) {
                predicates.add(cb.equal(root.get("academicYear"), academicYear.trim()));
            }
            if (region != null && !region.isBlank() && !"All".equalsIgnoreCase(region.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("institution").get("region")), region.trim().toUpperCase()));
            }
            if (stage != null && !stage.isBlank() && !"All".equalsIgnoreCase(stage.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("currentStage")), stage.trim().toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate refMatch = cb.like(cb.lower(root.get("applicationReference")), searchPattern);
                Predicate instMatch = cb.like(cb.lower(root.get("institution").get("name")), searchPattern);
                predicates.add(cb.or(refMatch, instMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
