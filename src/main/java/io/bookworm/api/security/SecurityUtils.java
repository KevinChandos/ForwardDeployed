package io.bookworm.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Static utility helpers for reading the current security principal.
 * <p>
 * Why: several layers (JPA auditing, service checks) need the current member UUID
 * without injecting the full security context; this class provides a single access
 * point that is easy to stub in unit tests.
 * <p>
 * Side effects: returns {@link Optional#empty()} whenever the context holds no
 * authenticated principal, which happens during unauthenticated guest requests
 * and scheduled batch jobs.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class — no instances
    }

    /**
     * Returns the UUID of the currently authenticated member, or empty if the
     * request is unauthenticated or the principal cannot be resolved to a UUID.
     */
    public static Optional<UUID> getCurrentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof BookwormUserDetails details) {
            return Optional.of(details.getMemberId());
        }
        return Optional.empty();
    }
}
