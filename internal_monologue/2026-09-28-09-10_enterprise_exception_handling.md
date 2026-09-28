# Interaction Summary

## Performed By
- `KevinChandos (AzureAD\KevinChandos)`

## Initial Prompt
> Generate enterprise-grade exception handling. Requirements: Problem Details — Global exception handler — Business exceptions — Validation exceptions — Security exceptions. Generate complete code in the appropriate solution structure.

## Objective
Implement a complete, production-ready exception-handling layer for the Bookworm Spring Boot 3 API, covering all required exception categories with RFC 7807-style typed Problem Details, MDC-backed correlation IDs, structured logging, and consistent JSON serialisation across the Spring Security filter chain and the MVC dispatcher.

## Repository Investigation
- `src/pom.xml` — Spring Boot 3.3.4, Java 21, JJWT 0.12.6, Lombok, MapStruct, Jakarta EE, springdoc 2.6.0.
- `src/main/.../common/exception/` — four pre-existing exceptions: `BusinessRuleException`, `ResourceNotFoundException`, `ResourceConflictException`, `AuthenticationFailedException`.
- `src/main/.../common/web/GlobalExceptionHandler.java` — previous handler used raw `Map<String,Object>` bodies, no logging, no MDC, no typed response class.
- `src/main/.../security/` — `JwtFilter`, `JwtAuthenticationEntryPoint`, `CustomAccessDeniedHandler` all used ad-hoc `LinkedHashMap` for error serialisation with no shared contract.
- `API/openapi.yaml` — `ErrorBody` schema with `code` (enum), `message`, `details[]`, `correlationId`, `timestamp`.

## Actions Taken
1. Created `ErrorDetail` record (`common/dto`) — typed field-level violation, replaces `Map<String,String>`.
2. Created `ErrorResponse` record (`common/dto`) — outer `{ "error": {...} }` envelope matching OpenAPI `ErrorBody`; static factory methods `of(...)` cover all call sites.
3. Upgraded `BusinessRuleException` — added `(errorCode, message, cause)` constructor for diagnostic chains.
4. Created `InsufficientStockException` — `BusinessRuleException` subclass with `bookId/requested/available`, HTTP 422 / `INSUFFICIENT_STOCK`.
5. Created `PaymentDeclinedException` — subclass carrying `gatewayCode`, HTTP 422 / `PAYMENT_DECLINED`.
6. Created `CouponExpiredException` — subclass carrying `couponCode`, HTTP 422 / `COUPON_INVALID`.
7. Created `ValidationException` — standalone (not subclassing `BusinessRuleException`) for service-layer multi-field failures; carries `List<ErrorDetail>`, HTTP 400.
8. Created `AccessDeniedException` — domain-level ownership guard, HTTP 403 / `FORBIDDEN`.
9. Created `TokenExpiredException` — signals JWT expiry distinctly from invalid token, HTTP 401 / `TOKEN_EXPIRED`.
10. Created `RateLimitExceededException` — carries `retryAfterSeconds`, HTTP 429 / `RATE_LIMIT_EXCEEDED`, sets `Retry-After` header.
11. Rewrote `GlobalExceptionHandler` — 14 handlers covering validation (5), security (5), resource (4), business (1), protocol (3), catch-all (1); typed `ErrorResponse`; MDC correlation ID; `X-Correlation-ID` header propagation.
12. Upgraded `JwtFilter` — catches `ExpiredJwtException` separately from `JwtException`; stores `TokenExpiredException` on request attribute for entry-point inspection.
13. Upgraded `JwtAuthenticationEntryPoint` — reads request attribute for `TokenExpiredException` to emit `TOKEN_EXPIRED`; uses `ErrorResponse` factory.
14. Upgraded `CustomAccessDeniedHandler` — uses `ErrorResponse` factory; removed raw `LinkedHashMap` construction.

## Validation
- `mvn compiler:compile` (from `src/` directory) — **passed with zero errors**.
- Pre-existing OpenAPI generator `[ERROR] Error resolving schema` lines in full `mvn compile` output are pre-existing issues in the OpenAPI spec and unrelated to this change.

## Models Used
- `claude-sonnet-4-5` (IBM Bob)

## Outputs
| File | Status |
|---|---|
| `src/main/java/io/bookworm/api/common/dto/ErrorDetail.java` | **Created** |
| `src/main/java/io/bookworm/api/common/dto/ErrorResponse.java` | **Created** |
| `src/main/java/io/bookworm/api/common/exception/BusinessRuleException.java` | **Modified** |
| `src/main/java/io/bookworm/api/common/exception/InsufficientStockException.java` | **Created** |
| `src/main/java/io/bookworm/api/common/exception/PaymentDeclinedException.java` | **Created** |
| `src/main/java/io/bookworm/api/common/exception/CouponExpiredException.java` | **Created** |
| `src/main/java/io/bookworm/api/common/exception/ValidationException.java` | **Created** |
| `src/main/java/io/bookworm/api/common/exception/AccessDeniedException.java` | **Created** |
| `src/main/java/io/bookworm/api/common/exception/TokenExpiredException.java` | **Created** |
| `src/main/java/io/bookworm/api/common/exception/RateLimitExceededException.java` | **Created** |
| `src/main/java/io/bookworm/api/common/web/GlobalExceptionHandler.java` | **Rewritten** |
| `src/main/java/io/bookworm/api/security/JwtFilter.java` | **Modified** |
| `src/main/java/io/bookworm/api/security/JwtAuthenticationEntryPoint.java` | **Modified** |
| `src/main/java/io/bookworm/api/security/CustomAccessDeniedHandler.java` | **Modified** |
