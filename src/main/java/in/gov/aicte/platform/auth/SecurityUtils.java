package in.gov.aicte.platform.auth;

import in.gov.aicte.platform.auth.dto.DevUserDto;
import in.gov.aicte.platform.institution.Institution;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enterprise Security Utility enforcing:
 * 1. Role-Based Access Control (RBAC) across 11 canonical AICTE governance roles
 * 2. Institutional Tenant Scoping (preventing cross-institution data access)
 * 3. Territorial Regional Scoping (preventing cross-region administrative leaks)
 */
@Component
public class SecurityUtils {

    public static final String ROLE_INSTITUTION_REPRESENTATIVE = "ROLE_INSTITUTION_REPRESENTATIVE";
    public static final String ROLE_REGIONAL_OFFICER = "ROLE_REGIONAL_OFFICER";
    public static final String ROLE_SCRUTINY_OFFICER = "ROLE_SCRUTINY_OFFICER";
    public static final String ROLE_EVC_COORDINATOR = "ROLE_EVC_COORDINATOR";
    public static final String ROLE_EVC_EXPERT = "ROLE_EVC_EXPERT";
    public static final String ROLE_COMPLIANCE_REVIEWER = "ROLE_COMPLIANCE_REVIEWER";
    public static final String ROLE_SENIOR_MANAGEMENT = "ROLE_SENIOR_MANAGEMENT";
    public static final String ROLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";
    public static final String ROLE_COMMITTEE_MEMBER = "ROLE_COMMITTEE_MEMBER";
    public static final String ROLE_LEGAL_OFFICER = "ROLE_LEGAL_OFFICER";
    public static final String ROLE_AUDITOR = "ROLE_AUDITOR";

    public Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public Optional<DevUserDto> getCurrentUser() {
        return getAuthentication()
                .map(Authentication::getDetails)
                .filter(details -> details instanceof DevUserDto)
                .map(details -> (DevUserDto) details);
    }

    public String getCurrentActorId() {
        return getAuthentication()
                .map(Authentication::getName)
                .orElse("SYSTEM");
    }

    public Set<String> getCurrentRoles() {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public boolean hasRole(String role) {
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase().replace(" ", "_");
        return getCurrentRoles().contains(target);
    }

    public boolean isAdministrator() {
        return hasRole(ROLE_ADMINISTRATOR);
    }

    public boolean isSeniorManagement() {
        return hasRole(ROLE_SENIOR_MANAGEMENT);
    }

    public boolean isAuthenticatedUser() {
        return getAuthentication()
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken))
                .isPresent();
    }

    /**
     * Enforce tenant isolation: Institution Representatives can only access records
     * belonging to their assigned institution.
     */
    public void assertInstitutionAccess(Institution institution) {
        if (institution == null || !isAuthenticatedUser() || isAdministrator() || isSeniorManagement() || hasRole("ROLE_USER")) {
            return;
        }

        if (hasRole(ROLE_INSTITUTION_REPRESENTATIVE)) {
            Optional<DevUserDto> userOpt = getCurrentUser();
            if (userOpt.isPresent()) {
                DevUserDto user = userOpt.get();
                String assignedInstId = user.institutionId();
                if (assignedInstId != null && !assignedInstId.isBlank()) {
                    boolean match = assignedInstId.equalsIgnoreCase(institution.getInstitutionalIdentifier())
                            || assignedInstId.equalsIgnoreCase(institution.getId().toString());
                    if (!match) {
                        throw new AccessDeniedException(String.format(
                                "Cross-institution access violation: User '%s' is restricted to institution '%s', but target is '%s'.",
                                user.name(), assignedInstId, institution.getInstitutionalIdentifier()
                        ));
                    }
                }
            }
        }
    }

    /**
     * Enforce territorial regional isolation: Regional Officers can only access records
     * belonging to their assigned territorial region.
     */
    public void assertRegionAccess(String targetRegion) {
        if (targetRegion == null || targetRegion.isBlank() || !isAuthenticatedUser() || isAdministrator() || isSeniorManagement() || hasRole("ROLE_USER")) {
            return;
        }

        if (hasRole(ROLE_REGIONAL_OFFICER)) {
            Optional<DevUserDto> userOpt = getCurrentUser();
            if (userOpt.isPresent()) {
                DevUserDto user = userOpt.get();
                String assignedRegion = user.region();
                if (assignedRegion != null && !assignedRegion.isBlank()
                        && !"All India".equalsIgnoreCase(assignedRegion)
                        && !"Multi-Region".equalsIgnoreCase(assignedRegion)
                        && !"Platform-wide".equalsIgnoreCase(assignedRegion)) {
                    if (!assignedRegion.equalsIgnoreCase(targetRegion.trim())) {
                        throw new AccessDeniedException(String.format(
                                "Cross-region access violation: Officer '%s' is restricted to '%s', but target belongs to '%s'.",
                                user.name(), assignedRegion, targetRegion
                        ));
                    }
                }
            }
        }
    }

    /**
     * Enforce workflow action-level role authorization.
     */
    public void assertWorkflowActionAllowed(String action) {
        if (action == null || !isAuthenticatedUser() || isAdministrator() || hasRole("ROLE_USER")) {
            return;
        }

        String normalizedAction = action.trim().toUpperCase();
        switch (normalizedAction) {
            case "RETURN_FOR_CORRECTION", "RESOLVE_DEFICIENCY", "UPDATE_CHECKLIST", "COMPLETE_SCRUTINY" -> {
                if (!hasRole(ROLE_SCRUTINY_OFFICER) && !hasRole(ROLE_REGIONAL_OFFICER) && !hasRole(ROLE_ADMINISTRATOR)) {
                    throw new AccessDeniedException(String.format(
                            "Unauthorized workflow transition: Action '%s' requires SCRUTINY_OFFICER, REGIONAL_OFFICER, or ADMINISTRATOR role.",
                            normalizedAction
                    ));
                }
            }

            case "SUBMIT_CORRECTIONS" -> {
                if (!hasRole(ROLE_INSTITUTION_REPRESENTATIVE) && !hasRole(ROLE_ADMINISTRATOR)) {
                    throw new AccessDeniedException(String.format(
                            "Unauthorized workflow transition: Action '%s' requires INSTITUTION_REPRESENTATIVE or ADMINISTRATOR role.",
                            normalizedAction
                    ));
                }
            }

            case "RECOMMEND_APPROVAL", "REJECT", "GRANT_APPROVAL" -> {
                if (!hasRole(ROLE_SENIOR_MANAGEMENT) && !hasRole(ROLE_COMMITTEE_MEMBER) && !hasRole(ROLE_ADMINISTRATOR)) {
                    throw new AccessDeniedException(String.format(
                            "Unauthorized workflow transition: Action '%s' requires SENIOR_MANAGEMENT, COMMITTEE_MEMBER, or ADMINISTRATOR role.",
                            normalizedAction
                    ));
                }
            }
        }
    }
}
