package in.gov.aicte.platform.auth.dto;

public record TokenResponse(
        String access_token,
        String token_type,
        DevUserDto user
) {
    public static TokenResponse of(String token, DevUserDto user) {
        return new TokenResponse(token, "bearer", user);
    }
}
