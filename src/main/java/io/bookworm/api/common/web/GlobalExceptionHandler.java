package io.bookworm.api.common.web;

import io.bookworm.api.common.dto.ErrorDetail;
import io.bookworm.api.common.dto.ErrorResponse;
import io.bookworm.api.common.exception.AccessDeniedException;
import io.bookworm.api.common.exception.AuthenticationFailedException;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.RateLimitExceededException;
import io.bookworm.api.common.exception.ResourceConflictException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.common.exception.TokenExpiredException;
import io.bookworm.api.common.exception.ValidationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotAllowedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Centralised exception handler that converts every exception category into
 * the OpenAPI {@code ErrorBody} envelope via typed {@link ErrorResponse} records.
 * <p>
 * Why: Keeps all HTTP error-shaping in one place — controllers stay clean, the OpenAPI
 * contract is always honoured, and every error event emits a structured log entry
 * with a {@code correlationId} that survives the full request cycle.
 * <p>
 * Side effects: Writes {@code correlationId} into the SLF4J MDC key "correlationId"
 * for the duration of the error-handling call so that downstream log aggregators can
 * join request traces to error records.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── MDC key ──────────────────────────────────────────────────────────────
    private static final String MDC_CORRELATION_ID = "correlationId";

    // ── Inbound correlation-ID header (forwarded by gateways / clients) ──────
    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION EXCEPTIONS                                             HTTP 400
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles {@code @Valid} / {@code @Validated} failures on request bodies.
     * Every {@code FieldError} is mapped to an {@link ErrorDetail} entry.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorDetail.of(
                        fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value",
                        fe.getRejectedValue()))
                .toList();

        ErrorResponse body = buildResponse("VALIDATION_ERROR", "Request validation failed", details, request);
        log.warn("[{}] Validation failed: {} field error(s)", correlationId(body), details.size());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles Jakarta Bean Validation constraint violations on query params and path variables.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(cv -> ErrorDetail.of(cv.getPropertyPath().toString(), cv.getMessage()))
                .toList();

        ErrorResponse body = buildResponse("VALIDATION_ERROR", "Constraint violation", details, request);
        log.warn("[{}] Constraint violation: {} violation(s)", correlationId(body), details.size());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles programmatic (service-layer) validation failures carrying a pre-built
     * list of {@link ErrorDetail} entries.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, WebRequest request) {

        ErrorResponse body = buildResponse("VALIDATION_ERROR", ex.getMessage(), ex.getErrors(), request);
        log.warn("[{}] Programmatic validation failed: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles malformed or unreadable JSON request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        ErrorResponse body = buildResponse("VALIDATION_ERROR", "Request body is malformed or missing", request);
        log.warn("[{}] Malformed request body: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles missing required query or form parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, WebRequest request) {

        ErrorDetail detail = ErrorDetail.of(ex.getParameterName(),
                String.format("Required parameter '%s' of type '%s' is missing",
                        ex.getParameterName(), ex.getParameterType()));
        ErrorResponse body = buildResponse("VALIDATION_ERROR", "Missing required request parameter",
                List.of(detail), request);
        log.warn("[{}] Missing parameter '{}': {}", correlationId(body), ex.getParameterName(), ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles type-mismatch on request parameters (e.g. string passed where UUID expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String type = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        ErrorDetail detail = ErrorDetail.of(ex.getName(),
                String.format("Parameter '%s' must be of type %s", ex.getName(), type),
                ex.getValue());
        ErrorResponse body = buildResponse("VALIDATION_ERROR", "Request parameter type mismatch",
                List.of(detail), request);
        log.warn("[{}] Type mismatch on '{}': {}", correlationId(body), ex.getName(), ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECURITY EXCEPTIONS                                         HTTP 401 / 403
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles application-layer authentication failures (bad credentials, revoked token, etc.).
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(
            AuthenticationFailedException ex, WebRequest request) {

        ErrorResponse body = buildResponse(ex.getErrorCode(), ex.getMessage(), request);
        log.warn("[{}] Authentication failed: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Handles expired JWT tokens — distinct from invalid tokens so clients can
     * automatically trigger the refresh-token flow.
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(
            TokenExpiredException ex, WebRequest request) {

        ErrorResponse body = buildResponse(ex.getErrorCode(), ex.getMessage(), request);
        log.warn("[{}] Token expired: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Handles domain-level access-denied exceptions (fine-grained ownership checks in services).
     * Named explicitly to disambiguate from Spring Security's same-named exception below.
     */
    @ExceptionHandler(io.bookworm.api.common.exception.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDomainAccessDenied(
            io.bookworm.api.common.exception.AccessDeniedException ex, WebRequest request) {

        ErrorResponse body = buildResponse(ex.getErrorCode(), ex.getMessage(), request);
        log.warn("[{}] Domain access denied: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Handles Spring Security infrastructure {@link AccessDeniedException} (role-based
     * filter / method-security rejections that bypass the entry point).
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {

        ErrorResponse body = buildResponse("FORBIDDEN",
                "Access denied: insufficient permissions to access this resource", request);
        log.warn("[{}] Spring Security access denied: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Handles rate-limit breaches.  Sets the standard {@code Retry-After} response header
     * when the exception carries a positive {@code retryAfterSeconds} value.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            RateLimitExceededException ex, WebRequest request) {

        ErrorResponse body = buildResponse(ex.getErrorCode(), ex.getMessage(), request);
        log.warn("[{}] Rate limit exceeded: {}", correlationId(body), ex.getMessage());

        HttpHeaders headers = new HttpHeaders();
        if (ex.getRetryAfterSeconds() > 0) {
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESOURCE EXCEPTIONS                                         HTTP 404 / 409
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles lookups for entities that do not exist in the persistence store.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        ErrorResponse body = buildResponse(ex.getErrorCode(), ex.getMessage(), request);
        log.info("[{}] Resource not found: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles uniqueness constraint violations detected at the service layer.
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ResourceConflictException ex, WebRequest request) {

        ErrorResponse body = buildResponse(ex.getErrorCode(), ex.getMessage(), request);
        log.warn("[{}] Resource conflict: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handles JPA optimistic-locking failures (concurrent writes to the same aggregate).
     * Returns 409 so the client can safely retry after re-fetching the resource.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException ex, WebRequest request) {

        ErrorResponse body = buildResponse("OPTIMISTIC_LOCK_CONFLICT",
                "The resource was modified by another request. Please fetch the latest version and retry.", request);
        log.warn("[{}] Optimistic lock conflict: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handles database-level unique constraint violations that were not caught earlier.
     * Last-resort guard — ideally services detect duplicates before hitting the DB.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, WebRequest request) {

        ErrorResponse body = buildResponse("CONFLICT",
                "A data integrity violation occurred. A duplicate entry may already exist.", request);
        log.error("[{}] Data integrity violation: {}", correlationId(body), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUSINESS EXCEPTIONS                                               HTTP 422
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles all {@link BusinessRuleException} subclasses (including
     * {@link io.bookworm.api.common.exception.InsufficientStockException},
     * {@link io.bookworm.api.common.exception.PaymentDeclinedException},
     * {@link io.bookworm.api.common.exception.CouponExpiredException}).
     * Spring's exception resolver selects the most specific handler first, so concrete
     * subclass handlers can be added here without touching this method.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, WebRequest request) {

        ErrorResponse body = buildResponse(ex.getErrorCode(), ex.getMessage(), request);
        log.warn("[{}] Business rule violation [{}]: {}", correlationId(body), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROTOCOL / ROUTING EXCEPTIONS                          HTTP 404 / 405 / 415
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles requests to paths that are not mapped by any controller.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(
            NoHandlerFoundException ex, WebRequest request) {

        ErrorResponse body = buildResponse("RESOURCE_NOT_FOUND",
                String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()), request);
        log.info("[{}] No handler found: {} {}", correlationId(body), ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles calls made with an HTTP method not supported by the matched endpoint.
     */
    @ExceptionHandler(HttpRequestMethodNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotAllowedException ex, WebRequest request) {

        ErrorResponse body = buildResponse("VALIDATION_ERROR",
                String.format("HTTP method '%s' is not supported. Allowed: %s",
                        ex.getMethod(), ex.getSupportedHttpMethods()), request);
        log.warn("[{}] Method not allowed: {}", correlationId(body), ex.getMessage());

        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedMethods() != null) {
            headers.addAll(HttpHeaders.ALLOW, List.of(ex.getSupportedMethods()));
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).body(body);
    }

    /**
     * Handles requests that send a {@code Content-Type} the endpoint does not consume.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {

        ErrorResponse body = buildResponse("VALIDATION_ERROR",
                String.format("Media type '%s' is not supported", ex.getContentType()), request);
        log.warn("[{}] Unsupported media type: {}", correlationId(body), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CATCH-ALL                                                         HTTP 500
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Last-resort handler — prevents stack-trace leakage from unhandled runtime exceptions.
     * Always logs at ERROR level with the full stack trace for post-incident diagnosis.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, WebRequest request) {

        ErrorResponse body = buildResponse("INTERNAL_ERROR",
                "An unexpected error occurred. Please try again or contact support.", request);
        log.error("[{}] Unhandled exception: {}", correlationId(body), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds an {@link ErrorResponse} without field-level details.
     * Attempts to reuse an inbound {@code X-Correlation-ID} header value so that
     * distributed tracing correlates the client request to the error response.
     */
    private ErrorResponse buildResponse(String code, String message, WebRequest request) {
        String cid = resolveCorrelationId(request);
        putMdc(cid);
        return ErrorResponse.of(code, message, null, cid);
    }

    /**
     * Builds an {@link ErrorResponse} with per-field validation details.
     */
    private ErrorResponse buildResponse(String code, String message,
                                        List<ErrorDetail> details, WebRequest request) {
        String cid = resolveCorrelationId(request);
        putMdc(cid);
        return ErrorResponse.of(code, message, details, cid);
    }

    /**
     * Extracts the correlation ID from the inbound header or generates a new UUID.
     * Using the client-supplied ID (when present) enables end-to-end trace correlation
     * across API gateways, load balancers, and front-end clients.
     */
    private String resolveCorrelationId(WebRequest request) {
        String inbound = request.getHeader(HEADER_CORRELATION_ID);
        return (inbound != null && !inbound.isBlank()) ? inbound : UUID.randomUUID().toString();
    }

    /**
     * Stores the correlation ID in the MDC so it appears in all log lines emitted
     * within the same thread during error handling.
     */
    private void putMdc(String correlationId) {
        MDC.put(MDC_CORRELATION_ID, correlationId);
    }

    /**
     * Extracts the correlation ID from an already-built response for log interpolation.
     */
    private String correlationId(ErrorResponse response) {
        return response.error() != null ? response.error().correlationId() : "n/a";
    }
}
