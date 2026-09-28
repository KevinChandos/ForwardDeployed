package io.bookworm.api.common.exception;

/**
 * Base unchecked exception for business rule violations across all bounded contexts.
 * <p>
 * Why: Allows domain and service layers to reject invalid business operations with
 * a clear programmatic error code and descriptive message that maps cleanly to HTTP 422.
 * <p>
 * Side effects: Triggers transaction rollback in Spring's transactional boundary.
 */
public class BusinessRuleException extends RuntimeException {

    private final String errorCode;

    public BusinessRuleException(String message) {
        super(message);
        this.errorCode = "BUSINESS_RULE_VIOLATION";
    }

    public BusinessRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor that preserves the originating cause for diagnostic chains.
     * Useful when wrapping lower-level checked exceptions at domain boundaries.
     */
    public BusinessRuleException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
