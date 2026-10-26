package in.gov.aicte.platform.auth;

import in.gov.aicte.platform.auth.dto.DevUserDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class DevBearerAuthenticationFilter extends OncePerRequestFilter {

    private final DevTokenService devTokenService;
    private final boolean devTokensEnabled;

    public DevBearerAuthenticationFilter(
            DevTokenService devTokenService,
            @Value("${app.security.dev-tokens-enabled:true}") boolean devTokensEnabled
    ) {
        this.devTokenService = devTokenService;
        this.devTokensEnabled = devTokensEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (devTokensEnabled && authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            String token = authHeader.substring(7).trim();
            Optional<DevUserDto> userOpt = devTokenService.validateToken(token);

            if (userOpt.isPresent()) {
                DevUserDto user = userOpt.get();
                String roleAuthority = "ROLE_" + user.role().toUpperCase().replace(" ", "_");
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleAuthority));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user.id(),
                        null,
                        authorities
                );
                authentication.setDetails(user);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
