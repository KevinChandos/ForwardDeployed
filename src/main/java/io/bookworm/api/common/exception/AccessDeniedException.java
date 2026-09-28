package io.bookworm.api.common.exception;

/**
 * Domain-level authorization exception thrown when an authenticated principal attempts an
 * action that their role does not permit (fine-grained resource ownership checks, etc.).
 * <p>
 * Why: Distinguishes application-layer authorization failures from Spring Security's
 * infrastructure-level {@link org.springframework.security.access.AccessDeniedException}.
 * Translated to HTTP 403 (FORBIDDEN) by {@code GlobalExceptionHandler}, allowing service
 * methods to reject requests based on domain rules rather than Spring Security annotations.
 * <p>
 * Side effects: Triggers transaction rollback.
 */
public class AccessDeniedException extends RuntimeException {

    private final String errorCode;

    public AccessDeniedException(String message) {
        super(message);
        this.errorCode = "FORBIDDEN";
    }

    public AccessDeniedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
