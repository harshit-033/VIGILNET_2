package in.gov.aicte.platform.auth.dto;

public record DevUserDto(
        String id,
        String name,
        String email,
        String role,
        String region,
        String institutionId,
        String institutionName,
        String designation
) {
}
