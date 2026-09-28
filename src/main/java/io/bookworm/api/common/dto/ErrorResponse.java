package io.bookworm.api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Typed envelope matching the OpenAPI {@code ErrorBody} schema.
 * <p>
 * Why: Replaces the raw {@code Map<String, Object>} pattern in the previous handler so that
 * response shaping is type-safe, serialisation is predictable, and the contract with OpenAPI
 * is enforced at compile time.  The outer object carries a single {@code error} property so
 * that {@code {"error":{...}}} wire format is preserved.
 * Side effects: None — immutable value type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        @JsonProperty("error")
        ErrorBody error
) {

    /**
     * Inner problem-detail object.  {@code details} is omitted from the JSON when null
     * (validation errors populate it; others leave it absent to keep responses lean).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(

            /**
             * Machine-readable error code matching the OpenAPI {@code ErrorBody.code} enum:
             * VALIDATION_ERROR | UNAUTHENTICATED | FORBIDDEN | RESOURCE_NOT_FOUND |
             * CONFLICT | BUSINESS_RULE_VIOLATION | RATE_LIMIT_EXCEEDED |
             * INTERNAL_ERROR | SERVICE_UNAVAILABLE | OPTIMISTIC_LOCK_CONFLICT | RESOURCE_DELETED.
             */
            String code,

            /** Short, human-readable description safe to return to the client. */
            String message,

            /**
             * Per-field violation details; present only for VALIDATION_ERROR responses.
             */
            List<ErrorDetail> details,

            /**
             * Unique identifier for this specific error event — correlates client logs
             * with server MDC trace entries.
             */
            String correlationId,

            /** ISO-8601 UTC instant when the error was generated. */
            OffsetDateTime timestamp
    ) {}

    // ── Static factory helpers ────────────────────────────────────────────────

    /**
     * Creates an {@code ErrorResponse} without per-field details (the common case).
     */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(
                new ErrorBody(code, message, null, UUID.randomUUID().toString(), OffsetDateTime.now())
        );
    }

    /**
     * Creates an {@code ErrorResponse} with per-field validation details.
     */
    public static ErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(
                new ErrorBody(code, message, details, UUID.randomUUID().toString(), OffsetDateTime.now())
        );
    }

    /**
     * Creates an {@code ErrorResponse} carrying a pre-assigned correlation ID
     * (e.g. extracted from the inbound {@code X-Correlation-ID} request header).
     */
    public static ErrorResponse of(String code, String message, List<ErrorDetail> details, String correlationId) {
        String cid = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : UUID.randomUUID().toString();
        return new ErrorResponse(
                new ErrorBody(code, message, details, cid, OffsetDateTime.now())
        );
    }
}
