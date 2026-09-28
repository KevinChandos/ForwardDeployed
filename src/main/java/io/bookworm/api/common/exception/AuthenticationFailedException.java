package io.bookworm.api.common.exception;

/**
 * Unchecked exception thrown when an authentication error occurs (e.g., bad password, revoked token).
 * <p>
 * Why: Translated to HTTP 401 (UNAUTHENTICATED) by GlobalExceptionHandler.
 * Side effects: Terminates security handshake and transaction.
 */
public class AuthenticationFailedException extends RuntimeException {

    private final String errorCode;

    public AuthenticationFailedException(String message) {
        super(message);
        this.errorCode = "UNAUTHENTICATED";
    }

    public AuthenticationFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
