package in.gov.aicte.platform.auth;

import in.gov.aicte.platform.auth.dto.DevUserDto;
import in.gov.aicte.platform.auth.dto.TokenRequest;
import in.gov.aicte.platform.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
    private final boolean devTokensEnabled;

    public AuthController(
            DevTokenService devTokenService,
            @Value("\${app.security.dev-tokens-enabled:true}") boolean devTokensEnabled
    ) {
        this.devTokenService = devTokenService;
        this.devTokensEnabled = devTokensEnabled;
    }

    @PostMapping("/token")
    @Operation(summary = "Issue development bearer token", description = "Generates an interim dev access token and user profile for Vite frontend demo")
    public ResponseEntity<TokenResponse> issueToken(@RequestBody(required = false) TokenRequest request) {
        if (!devTokensEnabled) {
            return ResponseEntity.notFound().build();
        }

        String username = request != null ? request.username() : null;
        String role = request != null ? request.role() : null;
        try {
            return ResponseEntity.ok(devTokenService.issueToken(username, role));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Returns active user profile from SecurityContext")
    public ResponseEntity<DevUserDto> getCurrentUser() {
        if (!devTokensEnabled) {
            return ResponseEntity.notFound().build();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // The development filter stores DevUserDto in Authentication.details;
        // the principal itself is the user's ID string.
        Object details = auth.getDetails();
        if (!(details instanceof DevUserDto userDto)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userDto);
    }
}
