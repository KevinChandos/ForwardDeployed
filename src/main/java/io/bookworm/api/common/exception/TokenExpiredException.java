package io.bookworm.api.common.exception;

/**
 * Thrown when a JWT token is structurally valid but has passed its expiration time.
 * <p>
 * Why: Separates token expiry (a recoverable, expected client condition handled with a
 * refresh-token flow) from genuinely invalid/tampered tokens, enabling the handler and
 * security layer to return distinct error codes and messages.
 * Mapped to HTTP 401 ({@code TOKEN_EXPIRED}) by {@code GlobalExceptionHandler}.
 * <p>
 * Side effects: {@code JwtFilter} catches {@link io.jsonwebtoken.ExpiredJwtException}
 * and re-throws this typed exception so the entry-point handler emits the right code.
 */
public class TokenExpiredException extends RuntimeException {

    private static final String ERROR_CODE = "TOKEN_EXPIRED";

    public TokenExpiredException() {
        super("Your session token has expired. Please re-authenticate or use the refresh endpoint.");
    }

    public TokenExpiredException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
