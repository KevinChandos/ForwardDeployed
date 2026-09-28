package io.bookworm.api.common.exception;

/**
 * Thrown when a client has exceeded the permitted request rate for an endpoint or operation.
 * <p>
 * Why: Provides a dedicated {@code RATE_LIMIT_EXCEEDED} error code (HTTP 429) that matches
 * the OpenAPI {@code ErrorBody.code} enum value.  The {@code retryAfterSeconds} field allows
 * the handler to set the standard {@code Retry-After} response header.
 * <p>
 * Side effects: None — rate-limit enforcement occurs before any persistence boundary is entered.
 */
public class RateLimitExceededException extends RuntimeException {

    private static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    /** Seconds the client should wait before retrying; 0 means "unspecified". */
    private final long retryAfterSeconds;

    public RateLimitExceededException() {
        super("Too many requests. Please slow down and retry later.");
        this.retryAfterSeconds = 0;
    }

    public RateLimitExceededException(long retryAfterSeconds) {
        super(String.format("Too many requests. Please retry after %d second(s).", retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
