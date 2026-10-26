package in.gov.aicte.platform.auth.dto;

public record TokenRequest(
        String username,
        String password,
        String role
) {
}
