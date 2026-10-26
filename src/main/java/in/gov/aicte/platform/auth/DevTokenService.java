package in.gov.aicte.platform.auth;

import in.gov.aicte.platform.auth.dto.DevUserDto;
import in.gov.aicte.platform.auth.dto.TokenResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development-only token and profile resolution service.
 * <p>
 * IMPORTANT SECURITY NOTICE: This service is an interim local testing bridge to enable
 * the Vite frontend prototype to authenticate against the backend. It does NOT represent
 * production security, and must be replaced with an approved OpenID Connect (OIDC) identity provider.
 */
@Service
public class DevTokenService {

    private final Map<String, DevUserDto> usersById = new ConcurrentHashMap<>();
    private final Map<String, DevUserDto> usersByRole = new ConcurrentHashMap<>();
    private final Map<String, DevUserDto> usersByEmail = new ConcurrentHashMap<>();

    private final Map<String, DevUserDto> activeTokens = new ConcurrentHashMap<>();

    public DevTokenService() {
        registerUser(new DevUserDto(
                "USR-INST-01",
                "Dr. Ramesh Kumar",
                "principal@abctech.edu.in",
                "Institution Representative",
                "Northern Region",
                "INST-1025",
                "ABC Institute of Technology",
                "Principal / Authorized Representative"
        ));
        registerUser(new DevUserDto(
                "USR-RO-01",
                "A. Sharma",
                "ro.north@aicte-india.gov.in",
                "Regional Officer",
                "Northern Region",
                null,
                null,
                "Regional Officer (North)"
        ));
        registerUser(new DevUserDto(
                "USR-SO-01",
                "B. Singh",
                "scrutiny.north@aicte-india.gov.in",
                "Scrutiny Officer",
                "Northern Region",
                null,
                null,
                "Scrutiny / Bureau Desk Officer"
        ));
        registerUser(new DevUserDto(
                "USR-EVC-01",
                "C. Verma",
                "evc.coord@aicte-india.gov.in",
                "EVC Coordinator",
                "Multi-Region",
                null,
                null,
                "EVC Operations Coordinator"
        ));
        registerUser(new DevUserDto(
                "USR-CR-01",
                "D. Patel",
                "compliance.rev@aicte-india.gov.in",
                "Compliance Reviewer",
                "Western Region",
                null,
                null,
                "Compliance & Evidence Reviewer"
        ));
        registerUser(new DevUserDto(
                "USR-MGT-01",
                "Prof. H. R. Adhikari",
                "advisor.approval@aicte-india.gov.in",
                "Senior Management",
                "All India",
                null,
                null,
                "Competent Authority / Advisor Approval"
        ));
        registerUser(new DevUserDto(
                "USR-ADM-01",
                "System Administrator",
                "admin@aicte-india.gov.in",
                "Administrator",
                "Platform-wide",
                null,
                null,
                "Technical Platform Administrator"
        ));
        registerUser(new DevUserDto(
                "USR-INST-02",
                "Dr. Sunita Rao",
                "principal@southpoly.edu.in",
                "Institution Representative",
                "Southern Region",
                "INST-2048",
                "Southern Polytechnic College",
                "Principal / Authorized Representative"
        ));
        registerUser(new DevUserDto(
                "USR-RO-02",
                "K. Ramanathan",
                "ro.south@aicte-india.gov.in",
                "Regional Officer",
                "Southern Region",
                null,
                null,
                "Regional Officer (South)"
        ));
        registerUser(new DevUserDto(
                "USR-EXP-01",
                "Dr. P. N. Murthy",
                "evc.expert@aicte-india.gov.in",
                "EVC Expert",
                "Multi-Region",
                null,
                null,
                "Senior Academic Peer Expert"
        ));
        registerUser(new DevUserDto(
                "USR-CM-01",
                "Justice (Retd.) S. Sen",
                "shc.member@aicte-india.gov.in",
                "Committee Member",
                "All India",
                null,
                null,
                "Standing Hearing Committee Member"
        ));
        registerUser(new DevUserDto(
                "USR-LO-01",
                "Adv. R. Narain",
                "legal@aicte-india.gov.in",
                "Legal Officer",
                "All India",
                null,
                null,
                "Legal Cell Officer"
        ));
        registerUser(new DevUserDto(
                "USR-AUD-01",
                "M. Sengupta",
                "auditor@aicte-india.gov.in",
                "Auditor",
                "Platform-wide",
                null,
                null,
                "Statutory Quality & Process Auditor"
        ));
    }

    private void registerUser(DevUserDto user) {
        usersById.put(user.id().toLowerCase(), user);
        usersByRole.put(user.role().toLowerCase(), user);
        usersByEmail.put(user.email().toLowerCase(), user);
    }

    public TokenResponse issueToken(String username, String role) {
        DevUserDto matched = null;

        if (username != null && !username.isBlank()) {
            String u = username.trim().toLowerCase();
            matched = usersByEmail.get(u);
            if (matched == null) {
                matched = usersById.get(u);
            }
            if (matched == null) {
                throw new IllegalArgumentException("Unknown development identity: " + username.trim());
            }
        }

        if (matched == null && role != null && !role.isBlank()) {
            matched = usersByRole.get(role.trim().toLowerCase());
        }

        if (matched == null) {
            throw new IllegalArgumentException("Unknown development role: " + (role == null ? "" : role.trim()));
        }

        String token = "dev-token-" + matched.id() + "-" + UUID.randomUUID().toString().substring(0, 8);
        activeTokens.put(token, matched);

        return TokenResponse.of(token, matched);
    }

    public Optional<DevUserDto> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        // Support persistent token format: dev-token-USR-SO-01-...
        DevUserDto user = activeTokens.get(token);
        if (user != null) {
            return Optional.of(user);
        }

        // Allow static dev format: dev-token-USR-SO-01
        if (token.startsWith("dev-token-")) {
            for (DevUserDto u : usersById.values()) {
                if (token.toLowerCase().contains(u.id().toLowerCase())) {
                    activeTokens.put(token, u);
                    return Optional.of(u);
                }
            }
        }

        return Optional.empty();
    }

    public List<DevUserDto> getAllDemoUsers() {
        return new ArrayList<>(usersById.values());
    }
}
