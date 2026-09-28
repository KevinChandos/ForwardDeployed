package io.bookworm.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bookworm.api.common.dto.ErrorResponse;
import io.bookworm.api.common.exception.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Authentication entry point invoked when an unauthenticated client requests a protected resource.
 * <p>
 * Why: Returns an OpenAPI {@code ErrorBody}-compatible JSON 401 Unauthorized response instead of
 * redirecting to a login page or emitting a blank HTML response.  Checks for a
 * {@link TokenExpiredException} stored as a request attribute by {@link JwtFilter} to surface the
 * more specific {@code TOKEN_EXPIRED} code (enabling automatic client-side refresh logic) rather
 * than the generic {@code UNAUTHENTICATED} code.
 * <p>
 * Side effects: Writes an {@link ErrorResponse} JSON payload directly to the HTTP response stream.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(), authException.getMessage());

        // Check whether the filter stashed a typed token exception on the request.
        // This enables TOKEN_EXPIRED to surface without the filter needing to write the response itself.
        Object tokenException = request.getAttribute("exception");
        ErrorResponse body;

        if (tokenException instanceof TokenExpiredException tee) {
            body = ErrorResponse.of(tee.getErrorCode(), tee.getMessage());
        } else {
            body = ErrorResponse.of("UNAUTHENTICATED", "Full authentication is required to access this resource");
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
