package io.bookworm.api.common.exception;

import io.bookworm.api.common.dto.ErrorDetail;

import java.util.List;

/**
 * Unchecked exception for programmatic (service-layer) validation failures that cannot be
 * expressed via Bean Validation annotations.
 * <p>
 * Why: Provides a first-class home for multi-field business validation errors (e.g. a date
 * range where {@code startDate > endDate}) that must reach the client as HTTP 400 with an
 * {@code errors} array, identical to the {@code @Valid} response shape.
 * <p>
 * Side effects: Triggers transaction rollback.  The embedded {@link ErrorDetail} list is
 * mapped directly into the {@code ErrorBody.details} array by {@code GlobalExceptionHandler}.
 */
public class ValidationException extends RuntimeException {

    private final List<ErrorDetail> errors;

    /**
     * Constructs a single-violation exception.
     *
     * @param field   dot-path of the offending field
     * @param message human-readable violation description
     */
    public ValidationException(String field, String message) {
        super(message);
        this.errors = List.of(ErrorDetail.of(field, message));
    }

    /**
     * Constructs a multi-violation exception, used when the service validates several
     * constraints in one pass before throwing (fail-all rather than fail-fast).
     *
     * @param errors pre-built list of field-level error details
     */
    public ValidationException(List<ErrorDetail> errors) {
        super("Request validation failed");
        this.errors = List.copyOf(errors);
    }

    public ValidationException(String message, List<ErrorDetail> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    /** Returns the immutable list of per-field errors to be included in the response. */
    public List<ErrorDetail> getErrors() {
        return errors;
    }
}
