package io.bookworm.api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single field-level error within a validation or business error response.
 * <p>
 * Why: Typed record eliminates raw {@code Map<String,String>} casts across the handler and
 * guarantees field/message are always serialised — matching the OpenAPI {@code ErrorDetail} schema.
 * Side effects: None — immutable value object.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(

        /** The dot-path field name that triggered the violation (e.g. "address.postalCode"). */
        String field,

        /** Human-readable violation description. */
        String message,

        /**
         * Optional rejected value, included only when safe to echo back to the client.
         * Never include sensitive values (passwords, tokens).
         */
        Object rejectedValue
) {

    /**
     * Convenience factory for simple field + message pairs (no rejected value).
     */
    public static ErrorDetail of(String field, String message) {
        return new ErrorDetail(field, message, null);
    }

    /**
     * Factory for field + message + rejected value triples.
     */
    public static ErrorDetail of(String field, String message, Object rejectedValue) {
        return new ErrorDetail(field, message, rejectedValue);
    }
}
