package io.bookworm.api.common.exception;

/**
 * Unchecked exception thrown when a requested resource is not found in the persistence store.
 * <p>
 * Why: Dispatched by repository query lookups in service implementations and translated
 * to HTTP 404 (RESOURCE_NOT_FOUND) by GlobalExceptionHandler.
 * Side effects: Triggers transaction rollback.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = "RESOURCE_NOT_FOUND";
    }

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
        this.errorCode = "RESOURCE_NOT_FOUND";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
