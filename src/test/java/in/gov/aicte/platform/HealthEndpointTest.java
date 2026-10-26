package in.gov.aicte.platform;

import in.gov.aicte.platform.config.SecurityConfig;
import in.gov.aicte.platform.health.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice test verifying the public health endpoint and security perimeter.
 * Decoupled from datasource infrastructure so web contract tests execute cleanly
 * while full application startup strictly enforces PostgreSQL availability.
 */
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, in.gov.aicte.platform.auth.DevBearerAuthenticationFilter.class, in.gov.aicte.platform.auth.DevTokenService.class})
class HealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health returns 200 OK with application availability status without authentication")
    void shouldReturnHealthStatusOk() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("sage-compliance-platform"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Unauthenticated access to any non-permitted route returns 401 Unauthorized")
    void shouldRejectUnauthenticatedAccessToProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/unpermitted-test-endpoint")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
