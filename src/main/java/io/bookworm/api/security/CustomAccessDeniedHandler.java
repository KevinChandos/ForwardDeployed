package io.bookworm.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bookworm.api.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for HTTP 403 Forbidden scenarios where the authenticated principal lacks
 * the required roles or permissions for the requested resource.
 * <p>
 * Why: Standardises Spring Security's {@link AccessDeniedException} into the uniform
 * {@link ErrorResponse} JSON envelope, keeping the wire format consistent with every other
 * error emitted by the application.
 * <p>
 * Side effects: Writes an {@link ErrorResponse} JSON payload directly to the HTTP response stream.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.warn("Access denied for URI {}: {}", request.getRequestURI(), accessDeniedException.getMessage());

        ErrorResponse body = ErrorResponse.of(
                "FORBIDDEN",
                "Access denied: insufficient permissions to access this resource"
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
