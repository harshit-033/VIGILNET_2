package in.gov.aicte.platform.auth;

import in.gov.aicte.platform.auth.dto.DevUserDto;
import in.gov.aicte.platform.auth.dto.TokenRequest;
import in.gov.aicte.platform.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication (Development Bridge)", description = "Interim development authentication endpoints for Vite frontend integration")
public class AuthController {

    private final DevTokenService devTokenService;

    public AuthController(DevTokenService devTokenService) {
        this.devTokenService = devTokenService;
    }

    @PostMapping("/token")
    @Operation(summary = "Issue development bearer token", description = "Generates an interim dev access token and user profile for Vite frontend demo")
    public ResponseEntity<TokenResponse> issueToken(@RequestBody(required = false) TokenRequest request) {
        String username = request != null ? request.username() : null;
        String role = request != null ? request.role() : null;
        return ResponseEntity.ok(devTokenService.issueToken(username, role));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Returns active user profile from SecurityContext")
    public ResponseEntity<DevUserDto> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof DevUserDto userDto) {
            return ResponseEntity.ok(userDto);
        }

        // Fallback default
        return ResponseEntity.ok(devTokenService.issueToken(null, "Regional Officer").user());
    }
}
