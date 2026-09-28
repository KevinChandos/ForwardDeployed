package io.bookworm.api.security;

import io.bookworm.api.common.exception.TokenExpiredException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter executed once per request to inspect the incoming Authorization header for a Bearer JWT.
 * <p>
 * Why: Intercepts HTTP requests, validates JWT signature and expiration via {@link JwtProvider},
 * and populates Spring Security's {@link SecurityContextHolder} with the authenticated principal.
 * Expired tokens re-throw as {@link TokenExpiredException} so the security entry point can
 * surface a distinct {@code TOKEN_EXPIRED} error code, enabling client-side refresh flows.
 * <p>
 * Side effects: Sets authentication object into SecurityContextHolder for valid tokens;
 * clears the context and delegates to the entry point for invalid or expired tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (jwtProvider.validateToken(token)) {
                    Authentication authentication = jwtProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Set Authentication to security context for '{}', uri: {}",
                            authentication.getName(), request.getRequestURI());
                }
            } catch (ExpiredJwtException ex) {
                // Re-map to a typed domain exception so GlobalExceptionHandler emits TOKEN_EXPIRED.
                // Clearing the context prevents an expired token from carrying partial state.
                SecurityContextHolder.clearContext();
                log.warn("Expired JWT token for URI {}: {}", request.getRequestURI(), ex.getMessage());
                request.setAttribute("exception", new TokenExpiredException());
            } catch (JwtException ex) {
                // Structurally invalid or tampered token — clear context and let the entry point handle it.
                SecurityContextHolder.clearContext();
                log.warn("Invalid JWT token for URI {}: {}", request.getRequestURI(), ex.getMessage());
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                log.error("Cannot set user authentication for URI {}: {}", request.getRequestURI(), ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw token value from the {@code Authorization: Bearer <token>} header.
     * Returns {@code null} when the header is absent or does not use the Bearer scheme.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
