package io.bookworm.api.common.exception;

/**
 * Unchecked exception thrown when a resource conflict is detected (e.g., unique email, duplicate SKU).
 * <p>
 * Why: Dispatched during duplicate validation checks and translated to HTTP 409 (CONFLICT).
 * Side effects: Triggers transaction rollback.
 */
public class ResourceConflictException extends RuntimeException {

    private final String errorCode;

    public ResourceConflictException(String message) {
        super(message);
        this.errorCode = "CONFLICT";
    }

    public ResourceConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
