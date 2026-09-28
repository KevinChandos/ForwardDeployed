# BookWorm E-Store — Architecture Review Report

> **Reviewer:** Principal Architect  
> **Date:** 2026-09-28  
> **Scope:** Full-stack review — Spring Boot 3 backend, PostgreSQL on RDS, AWS App Runner, CloudFormation IaC, GitHub Actions CI/CD, DDD design, OpenAPI contract, security posture, and operational readiness  
> **Artifacts Reviewed:** `src/`, `Architecture/`, `deploy/cloudformation/`, `.github/workflows/`, `Dockerfile`, `docker-compose.yml`, `sonar-project.properties`, `Requirements/`

---

## Table of Contents

1. [Architecture Scorecard](#1-architecture-scorecard)
2. [Risk Register](#2-risk-register)
3. [Improvement Recommendations](#3-improvement-recommendations)
4. [Production Readiness Checklist](#4-production-readiness-checklist)

---

## 1. Architecture Scorecard

Each dimension is scored 1–5 (5 = production-grade, no gaps) with a short rationale.

| Dimension | Score | Verdict |
|---|---|---|
| Security | 4 / 5 | Strong posture — one critical gap |
| Scalability | 3 / 5 | Good start; ceiling hit at DB layer |
| Reliability | 3.5 / 5 | Multi-AZ covered; outbox relay is incomplete |
| Performance | 3 / 5 | No caching layer; N+1 risk in order list |
| Cost | 4 / 5 | Well-sized; one idle-waste opportunity |
| Observability | 4 / 5 | Solid alarms; no SLO/SLI definition |
| Maintainability | 4.5 / 5 | Best-in-class DDD, contract-first, test pyramid |

**Overall: 3.7 / 5 — Conditionally production-ready with three must-fix items.**

---

### 1.1 Security — 4 / 5

**Strengths**
- JWT access tokens are short-lived (15 min); refresh tokens are server-side with full revocation on password reset and logout.
- Token storage is safe: only SHA-256 hashes stored in `identity.sessions` — the raw token never persists in the DB.
- BCrypt with work factor 12 for credential hashing — appropriate.
- IAM roles follow least-privilege; no wildcard resource ARNs on the instance role.
- All AWS service calls (Secrets Manager, ECR, X-Ray, CloudWatch) route via Interface VPC Endpoints — traffic never touches the public internet.
- No secrets in source code; no secrets in CloudWatch logs; no secrets in the container image.
- KMS customer-managed keys for Secrets Manager secrets; AWS-managed AES-256 for RDS and ECR at rest.
- HMAC-SHA256 webhook validation for `/payments/webhook`.
- CORS configured to named origins; `*` wildcard explicitly rejected.
- Container runs as non-root UID 1001; minimal `eclipse-temurin:21-jre-alpine` base image.
- ECR Enhanced Scanning (Inspector) blocks critical/high CVEs before deploy.

**Gaps**
- **`/cart/**` is fully `permitAll()`** in [`SecurityConfig.java`](../src/main/java/io/bookworm/api/config/SecurityConfig.java:142). There is no rate limiting, no abuse detection, and no guest-token entropy enforcement at the security layer. A malicious actor can create unlimited guest carts, exhausting DB connections without any throttle.
- The `GuestTokenFilter` is designed but the security filter chain shown does not enforce any origin or size constraints on `X-Guest-Token` values (arbitrary UUIDs accepted).
- `RateLimitExceededException` has a handler in [`GlobalExceptionHandler.java`](../src/main/java/io/bookworm/api/common/web/GlobalExceptionHandler.java:216) but **no rate-limiting implementation exists** in the codebase — no `Bucket4j`, `Resilience4j RateLimiter`, or API Gateway layer. The exception class is a dead letter.
- The `application.yml` default `JWT_SECRET` is the placeholder `CHANGE_ME_IN_PRODUCTION_USE_256_BIT_KEY` — if the production environment ever starts without a proper override, the JWT secret is publicly known.
- Swagger UI (`/v3/api-docs/**`, `/swagger-ui/**`) is permanently `permitAll()` — should be blocked in production profile.

---

### 1.2 Scalability — 3 / 5

**Strengths**
- Stateless JWT authentication enables horizontal App Runner scaling with no session affinity.
- App Runner auto-scaling configured: min 1, max 5 instances, 100 concurrent requests/instance (500 total RPS ceiling without state changes).
- Read replica offloads catalogue, search, and recommendations reads.
- Domain-per-package design means the monolith can be surgically extracted into microservices if a domain becomes a bottleneck.
- Transactional Outbox pattern (`outbox.domain_event_outbox`) provides the backbone for eventual decoupling without requiring synchronous calls.

**Gaps**
- **Single-writer RDS `db.t4g.medium`** (2 vCPU / 4 GB) is the hard write ceiling. The checkout → wallet debit → coupon increment → cart soft-delete flow is a single serialised transaction against the primary. Under Black Friday-level load this becomes the first bottleneck. No connection pooling middleware (PgBouncer or RDS Proxy) is present.
- **No application-level cache** (Redis/ElastiCache). Catalogue reads, category trees, and recommendation profiles hit the DB on every request. This is the fastest path to degraded UX at moderate traffic.
- App Runner max 5 instances is hard-coded in the CloudFormation parameter default — production peak traffic projections are not documented, so it is unclear whether 5 is adequate.
- `getMemberOrders()` in [`OrderServiceImpl.java`](../src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java:423) issues **N+1 queries**: for each order in the page it fires two additional queries (payment status, shipment status). At page size 20 this is 41 DB calls per list request.
- The recommendation engine is backed by a PostgreSQL table (`discovery.recommended_books`), not a dedicated vector or graph store. This will not scale beyond simple rule-based recommendations.

---

### 1.3 Reliability — 3.5 / 5

**Strengths**
- RDS Multi-AZ with synchronous standby in a second AZ — automatic failover in ~60 s.
- App Runner health checks every 10 s; unhealthy threshold of 5 (50 s to remove a failing instance).
- Rollback strategy is well-documented: `:stable` ECR tag, GitHub Actions `rollback.yml`, 2–5 min recovery window.
- Optimistic locking (`@Version`) on mutable entities prevents silent data corruption under concurrent writes.
- Flyway schema migrations run before application boot — no manual DDL steps.
- Password reset and session invalidation are transactional — no partial state.

**Gaps**
- **Transactional Outbox relay is not implemented.** The `DomainEventOutbox` entity and repository exist, but there is no `@Scheduled` relay process, no SQS/SNS/Kafka publisher, and no FAILED-row alerting. Domain events written to the outbox (e.g. `OrderPlaced`, `PaymentConfirmed`) are never read out — effectively a silent data sink. This means the notification module and any downstream consumers are non-functional.
- **No circuit breaker or retry** around the payment gateway call. If the payment gateway times out, the checkout session expires and the member loses their checkout state with no recovery path surfaced.
- `confirmOrder()` in [`OrderServiceImpl.java`](../src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java:247) accepts an `idempotencyKey` parameter but **does not use it** — the idempotency key is logged but never stored or checked. Double-submits will create duplicate orders.
- The `getSessionStore()` fallback in [`OrderServiceImpl.java`](../src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java:538) silently falls back to `storeRepository.findAll().get(0)` if no default store is found — non-deterministic behaviour in a multi-tenant system.
- 7-day PITR backup retention is minimal for a payment-processing system; PCI-DSS guidance recommends 90 days for audit logs.

---

### 1.4 Performance — 3 / 5

**Strengths**
- Read replica for catalogue/search/recommendations — separates write contention from read traffic.
- `@Transactional(readOnly = true)` class-level annotation on services — Hibernate flushes are skipped on read paths.
- MapStruct compile-time mapping — zero runtime reflection overhead.
- PostgreSQL index design covers FK columns, soft-delete filters (`deleted_at IS NULL`), and common search predicates.
- `NUMERIC(19,4)` for money — correct precision without floating-point error.
- JVM flags: `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError` respect cgroup limits.

**Gaps**
- **N+1 query in `getMemberOrders()`** — see Scalability §1.2. This degrades linearly with order history length.
- **No HTTP response caching headers** on catalogue endpoints — clients and CDN cannot cache GET `/books/{id}` or `/categories` responses.
- **No connection pool configuration** in `application.yml` or the CloudFormation template. HikariCP defaults (10 connections) against a `db.t4g.medium` (max ~90 connections) may cause pool exhaustion under moderate load when multiple App Runner instances are running.
- Catalogue search (`/search/**`) uses JPA `Specification` against PostgreSQL — full-text search is not leveraging `tsvector`/`tsquery` or an external search engine like OpenSearch. Complex filters will produce expensive sequential scans at scale.
- App Runner cold start for a Spring Boot 3 + Hibernate 6 JAR is typically 15–30 s. With `AutoScalingMinSize: 1`, scale-out events will expose this latency to end users.

---

### 1.5 Cost — 4 / 5

**Strengths**
- App Runner pay-per-use billing (no idle cluster cost vs. ECS/EKS).
- ECR lifecycle policies trim untagged images after 1 day; keep only last 5–10 release images.
- `db.t4g.medium` Graviton2 instances offer 20% cost savings over equivalent x86 classes.
- Cost estimate is documented and broken down per service in the Architecture document.
- Non-production environments support `AutoScalingMinSize: 0` to pause idle compute.
- `gp3` storage type for RDS — cost-effective vs. `gp2` at the same IOPS tier.

**Gaps**
- CloudWatch Logs retention is not documented as lifecycle-controlled — application logs accumulate indefinitely and are a significant cost accumulator at scale.
- No Savings Plans or Reserved Instances are discussed. At sustained 24/7 production load, a 1-year App Runner compute commitment would reduce costs by approximately 17%.
- X-Ray sampling rate is not configured — default 5% reservoir + 5% fixed rate may be undershooting at low traffic (generating noise) or hitting the 5 traces/s AWS free tier and incurring costs at high traffic.

---

### 1.6 Observability — 4 / 5

**Strengths**
- Correlation ID propagated through every HTTP request, error response, and MDC log context — enables trace joining without full OpenTelemetry.
- `X-Correlation-ID` round-tripped on every response — clients can correlate their errors.
- CloudWatch alarms defined for: 5xx rate, P99 latency, max instances hit, RDS CPU, RDS connections, RDS storage, replica lag.
- CloudTrail enabled — all AWS API calls (IAM, Secrets Manager, ECR, App Runner, RDS) are audited.
- RDS `log_min_duration_statement = 1000ms` surfaces slow queries.
- AWS X-Ray distributed tracing integrated at App Runner + App level.
- Structured logging with Slf4j + MDC — all log lines carry `correlationId`.
- SonarCloud quality gate blocks pipeline on coverage/smell threshold.

**Gaps**
- **No SLO/SLI definitions.** Alarms exist but there is no documented error budget, availability target, or latency SLO. Without these, the alarms lack thresholds grounded in business expectations.
- **No business-metric instrumentation** — there are no custom CloudWatch metrics for: checkout conversion rate, payment success rate, coupon redemption rate, or cart abandonment. These are critical for a commerce platform.
- Structured log format is not enforced — plain-text log output will make log aggregation (Athena, OpenSearch) expensive and brittle.
- The outbox `FAILED` status has no associated CloudWatch alarm — FAILED events silently accumulate.
- No distributed tracing across the recommendation or outbox relay paths because the relay is not implemented.

---

### 1.7 Maintainability — 4.5 / 5

**Strengths**
- Contract-first OpenAPI generator strategy enforces zero API drift — spec change = compile failure.
- Domain-per-package (`auth`, `cart`, `catalogue`, `checkout`, etc.) — cohesion is high; future extraction to microservices is surgical.
- Single `@Transactional` boundary at application layer — no leaking transaction scope.
- Comprehensive exception taxonomy in `common/exception/` — all domain errors are typed.
- MapStruct compile-time mappers with Lombok annotation processor ordering — no silent mapping bugs.
- Testcontainers-backed integration tests run against real PostgreSQL — migration scripts tested in CI.
- Full testing pyramid: `@WebMvcTest`, unit tests, `@DataJpaTest`, `@SpringBootTest`.
- All 40+ tables carry audit columns (`created_at`, `created_by`, `updated_at`, `updated_by`).
- Soft-delete pattern applied uniformly with `deleted_at` columns.
- Flyway plain-SQL migrations — readable by non-Java team members.
- CODEOWNERS + branch protection implied by CI/CD design.
- CloudFormation IaC with per-environment parameterisation — infrastructure is reproducible.

**Gaps**
- **Token TTL constants are hard-coded** as `static final long` in [`AuthServiceImpl.java`](../src/main/java/io/bookworm/api/auth/application/AuthServiceImpl.java:58) independently of the `application.yml` config properties — a config change in YAML will not affect runtime behaviour without also changing the constant.
- No API versioning strategy beyond the path prefix `/v1`. The OpenAPI spec defines no deprecation or sunset mechanism.
- `architecture.version` is missing from the `pom.xml` — the `1.0.0-SNAPSHOT` version should be promoted to a release tag at production deploy time.
- The `Dockerfile` OCI labels reference `"my-app"` and `"My Organisation"` — placeholder values not replaced with real project metadata.

---

## 2. Risk Register

Risks are classified by Likelihood (L) and Impact (I) on a 1–3 scale. **Risk Score = L × I**.

| ID | Risk | Likelihood | Impact | Score | Category |
|---|---|---|---|---|---|
| R-01 | `confirmOrder()` idempotency key unused — double-submit creates duplicate orders and double wallet debits | Medium (2) | High (3) | **6** | Reliability / Financial |
| R-02 | Rate limiting is a no-op — `RateLimitExceededException` exists but no enforcer; `/cart/**` is fully open | High (3) | Medium (2) | **6** | Security / Reliability |
| R-03 | Outbox relay not implemented — `OrderPlaced`, `PaymentConfirmed` domain events are never published; notification module and downstream integrations are silent | High (3) | Medium (2) | **6** | Reliability |
| R-04 | N+1 queries in `getMemberOrders()` — 41 DB calls per page of 20 orders under member order history growth | High (3) | Medium (2) | **6** | Performance |
| R-05 | HikariCP default pool (10 connections) × 5 App Runner instances = 50 connections; `db.t4g.medium` max_connections ≈ 90; headroom is razor-thin under load | Medium (2) | High (3) | **6** | Reliability / Performance |
| R-06 | No caching — every catalogue browse request hits RDS; DB becomes a bottleneck at modest concurrent user counts | Medium (2) | Medium (2) | **4** | Performance / Scalability |
| R-07 | JWT secret placeholder fallback — `CHANGE_ME_IN_PRODUCTION_USE_256_BIT_KEY` is the default; a misconfigured deploy creates a known JWT secret | Low (1) | High (3) | **3** | Security |
| R-08 | Swagger UI permanently public — internal API schema visible in production | Medium (2) | Low (1) | **2** | Security |
| R-09 | `getSessionStore()` non-deterministic fallback in multi-tenant context — order placed against wrong store silently | Low (1) | High (3) | **3** | Data Integrity |
| R-10 | Payment gateway has no circuit breaker — gateway timeout causes checkout session expiry with no recovery path | Low (1) | High (3) | **3** | Reliability |
| R-11 | 7-day PITR backup retention may not satisfy financial audit requirements | Low (1) | Medium (2) | **2** | Compliance |
| R-12 | Recommendation engine backed by PostgreSQL table — not a graph/vector store; accuracy ceiling is low | Low (1) | Low (1) | **1** | Scalability |

---

## 3. Improvement Recommendations

Recommendations are ordered by priority tier.

---

### Tier 1 — Must Fix Before Production (Critical)

#### REC-01 — Implement Order Idempotency (addresses R-01)

**File:** [`src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java`](../src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java:247)

The `idempotencyKey` parameter is received in `confirmOrder()` but never checked against a stored record. Add an `idempotency_keys` table (or use the existing `outbox` schema) keyed on `(idempotency_key, member_id)`. On receipt of a duplicate key, replay the previously committed `OrderConfirmationResponse` without re-executing the transaction.

```java
// Before creating the order, check for an existing idempotency record
Optional<IdempotencyRecord> existing = idempotencyRepository.findByKey(idempotencyKey);
if (existing.isPresent()) {
    return deserialize(existing.get().getResponsePayload(), OrderConfirmationResponse.class);
}
// ... proceed with order creation ...
// After committing, store the idempotency record
idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, memberId, serialize(response)));
```

---

#### REC-02 — Implement Rate Limiting (addresses R-02)

The `RateLimitExceededException` and handler are in place — the enforcer is missing. Add Bucket4j (already aligned with Spring Boot 3) as a filter:

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

Apply limits at minimum to:
- `POST /auth/login` — 5 attempts / 1 minute per IP (brute-force protection)
- `POST /auth/password/forgot` — 3 attempts / 1 hour per email (account enumeration)
- `POST /cart/**`, `GET /cart/**` — 60 requests / 1 minute per guest token (cart abuse)

---

#### REC-03 — Implement the Outbox Relay (addresses R-03)

The `DomainEventOutbox` entity and repository exist. Add a `@Scheduled` relay bean:

```java
@Scheduled(fixedDelay = 5000)
@Transactional
public void relayPendingEvents() {
    List<DomainEventOutbox> pending = outboxRepository.findTopNByStatusOrderByOccurredAt(
        OutboxStatus.PENDING, 50);
    for (DomainEventOutbox event : pending) {
        try {
            eventBus.publish(event.getEventType(), event.getPayload());
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(OffsetDateTime.now());
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            if (event.getRetryCount() >= MAX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED);
            }
        }
        outboxRepository.save(event);
    }
}
```

Add a CloudWatch alarm on `SELECT COUNT(*) FROM outbox.domain_event_outbox WHERE status = 'FAILED'` > 0.

---

#### REC-04 — Fix N+1 in `getMemberOrders()` (addresses R-04)

**File:** [`src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java`](../src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java:423)

Replace the per-order loop with a batch query:

```java
// Single query: fetch all order IDs for the page, then batch-fetch related data
List<UUID> orderIds = summaries.stream().map(Order::getOrderId).toList();
Map<UUID, String> paymentStatuses = paymentTransactionRepository.findLatestStatusByOrderIds(orderIds);
Map<UUID, String> shipmentStatuses = shipmentRepository.findLatestStatusByOrderIds(orderIds);
```

Add JPQL or native queries returning `Map<UUID, String>` keyed on `orderId`. This reduces 41 DB calls to 3.

---

#### REC-05 — Configure HikariCP Pool Size (addresses R-05)

Add explicit pool sizing to `application.yml` and the production profile:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 15        # Per instance; 5 instances × 15 = 75 connections
      minimum-idle: 5
      connection-timeout: 30000    # 30s before throwing PoolTimeout
      idle-timeout: 600000         # 10 min idle before eviction
      max-lifetime: 1800000        # 30 min max connection lifetime
```

For production: evaluate adding **RDS Proxy** in front of the primary instance to absorb connection spikes across App Runner scale-out events.

---

### Tier 2 — Should Fix Before General Availability

#### REC-06 — Add a Caching Layer (addresses R-06)

Add ElastiCache (Redis) for:

| Cache key | TTL | Data |
|---|---|---|
| `catalogue:book:{bookId}` | 15 min | `BookDetailResponse` |
| `catalogue:category-tree` | 60 min | `CategoryTreeResponse` |
| `discovery:recommendations:{memberId}` | 30 min | `RecommendationResponse` |

Spring Cache abstraction (`@Cacheable`, `@CacheEvict`) with `spring-boot-starter-cache` + `spring-data-redis` keeps the service layer clean. Estimated DB read reduction: 60–80% on catalogue endpoints.

---

#### REC-07 — Fix Token TTL Hardcoding (addresses Maintainability gap)

**File:** [`src/main/java/io/bookworm/api/auth/application/AuthServiceImpl.java`](../src/main/java/io/bookworm/api/auth/application/AuthServiceImpl.java:58)

```java
// Replace:
private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900L;
private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 604800L;

// With injected config:
@Value("${bookworm.jwt.access-token-ttl-seconds:900}")
private long accessTokenTtlSeconds;

@Value("${bookworm.jwt.refresh-token-ttl-seconds:604800}")
private long refreshTokenTtlSeconds;
```

---

#### REC-08 — Block Swagger UI in Production Profile

**File:** [`src/main/java/io/bookworm/api/config/SecurityConfig.java`](../src/main/java/io/bookworm/api/config/SecurityConfig.java:93)

```java
// Conditionally expose API docs only in non-production profiles
.requestMatchers(
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
).access(new WebExpressionAuthorizationManager(
    "hasRole('PLATFORM_ADMIN') or @environment.acceptsProfiles('!prod')"
))
```

---

#### REC-09 — Define SLOs and Wire Them to Alarms

Document in `Architecture/SLO.md`:
- **Availability:** 99.9% monthly uptime (≤ 43.8 min downtime/month)
- **Latency:** P99 < 500 ms for catalogue reads; P99 < 2 s for checkout confirm
- **Error rate:** < 0.1% 5xx on any rolling 5-minute window

Update the CloudWatch alarm thresholds in [`deploy/cloudformation/cloudwatch.yaml`](../deploy/cloudformation/cloudwatch.yaml) to reflect these targets, not arbitrary defaults.

---

#### REC-10 — Add Payment Circuit Breaker (addresses R-10)

Wrap the payment gateway client with Resilience4j:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
```

Configure a `CircuitBreaker` with:
- `slidingWindowSize: 10`, `failureRateThreshold: 50`
- `waitDurationInOpenState: 30s`
- Fallback: return `PAYMENT_GATEWAY_UNAVAILABLE` to the client with a `503` and `Retry-After: 30`.

---

### Tier 3 — Technical Debt / Future

#### REC-11 — Add JSON Structured Logging

Replace the default Spring Boot logback format with a JSON encoder for CloudWatch Logs Insights:

```xml
<!-- logback-spring.xml -->
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
  <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

This enables Athena queries and CloudWatch Insights queries on `correlationId`, `memberId`, `level`, and custom MDC fields without regex parsing.

---

#### REC-12 — Fix Dockerfile OCI Labels

**File:** [`Dockerfile`](../Dockerfile:39)

```dockerfile
# Replace:
LABEL org.opencontainers.image.title="my-app"
LABEL org.opencontainers.image.vendor="My Organisation"

# With:
LABEL org.opencontainers.image.title="bookworm-api"
LABEL org.opencontainers.image.vendor="BookWorm"
LABEL org.opencontainers.image.source="https://github.com/bookworm/bookworm-api"
```

---

#### REC-13 — Extend RDS Backup Retention to 30 Days

**File:** [`deploy/cloudformation/rds.yaml`](../deploy/cloudformation/rds.yaml)

Change `BackupRetentionPeriod` from `7` to `30` (minimum for financial records). Enable automated snapshots export to S3 for long-term audit retention at low cost.

---

#### REC-14 — Add `getSessionStore()` Guard (addresses R-09)

**File:** [`src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java`](../src/main/java/io/bookworm/api/checkout/application/OrderServiceImpl.java:538)

```java
private Store getSessionStore(CheckoutSession session) {
    Store store = storeRepository.findActiveDefaultStore()
        .orElseThrow(() -> new BusinessRuleException("NO_DEFAULT_STORE",
            "No active default store configured. Contact platform support."));
    return store;
}
```

Remove the silent `findAll().get(0)` fallback — it masks a misconfiguration and produces non-deterministic order routing.

---

## 4. Production Readiness Checklist

### 4.1 Security ✅ / ⚠️ / ❌

| # | Check | Status | Notes |
|---|---|---|---|
| S-01 | JWT secret stored in Secrets Manager, not environment variable | ✅ | Rotated every 30 days |
| S-02 | JWT access token TTL ≤ 15 min | ✅ | 900 s |
| S-03 | Refresh token stored as hash only | ✅ | SHA-256 in `identity.sessions` |
| S-04 | BCrypt password hashing with adequate work factor | ✅ | Factor 12 |
| S-05 | RDS not publicly accessible | ✅ | Private subnet only |
| S-06 | All AWS service calls via VPC Endpoints | ✅ | Secrets Manager, ECR, X-Ray, CloudWatch |
| S-07 | Container runs as non-root | ✅ | UID 1001 |
| S-08 | ECR image scanning enabled | ✅ | Enhanced Scanning (Inspector) |
| S-09 | HMAC webhook validation | ✅ | HMAC-SHA256 |
| S-10 | CORS configured to named origins | ✅ | No wildcard |
| S-11 | SQL injection prevention | ✅ | JPA/JPQL bound params only |
| S-12 | Input validation on all endpoints | ✅ | JSR-380 + `spring-boot-starter-validation` |
| S-13 | Swagger UI blocked in production | ❌ | **Must fix — see REC-08** |
| S-14 | Rate limiting implemented | ❌ | **Must fix — see REC-02** |
| S-15 | Guest cart abuse protection | ❌ | **Must fix — see REC-02** |
| S-16 | JWT secret startup guard | ⚠️ | Default placeholder accepted; add startup assertion |
| S-17 | KMS CMK for Secrets Manager | ✅ | `bookworm-prod-secrets-cmk` |
| S-18 | CloudTrail enabled | ✅ | All AWS API calls logged |

---

### 4.2 Reliability ✅ / ⚠️ / ❌

| # | Check | Status | Notes |
|---|---|---|---|
| R-01 | RDS Multi-AZ enabled in production | ✅ | CloudFormation param `MultiAZEnabled: true` |
| R-02 | RDS deletion protection enabled | ✅ | |
| R-03 | App Runner health check configured | ✅ | 10 s interval, `/v1/actuator/health` |
| R-04 | Rollback procedure documented and tested | ✅ | `docs/rollback-strategy.md`, `rollback.yml` |
| R-05 | Flyway migrations run at boot (no manual DDL) | ✅ | |
| R-06 | Optimistic locking on mutable aggregates | ✅ | `@Version` on entities |
| R-07 | Password reset invalidates all sessions | ✅ | `softDeleteAllByMemberId()` |
| R-08 | Order idempotency key enforced | ❌ | **Must fix — see REC-01** |
| R-09 | Outbox relay operational | ❌ | **Must fix — see REC-03** |
| R-10 | Payment gateway circuit breaker | ⚠️ | Recommended before GA — see REC-10 |
| R-11 | `getSessionStore()` deterministic | ❌ | **Must fix — see REC-14** |
| R-12 | HikariCP pool size explicitly configured | ❌ | **Must fix — see REC-05** |
| R-13 | Backup retention ≥ 30 days for financial data | ⚠️ | Currently 7 days — see REC-13 |
| R-14 | Connection pool fits within RDS max_connections | ⚠️ | Default HikariCP 10 × 5 instances = 50 — tight |

---

### 4.3 Performance ✅ / ⚠️ / ❌

| # | Check | Status | Notes |
|---|---|---|---|
| P-01 | Read replica configured for catalogue/search | ✅ | Separate `DataSource` bean |
| P-02 | `@Transactional(readOnly = true)` on service classes | ✅ | All service impls annotated |
| P-03 | PostgreSQL indexes on FK and filter columns | ✅ | Comprehensive index design per schema |
| P-04 | N+1 query in `getMemberOrders()` resolved | ❌ | **Must fix — see REC-04** |
| P-05 | HTTP caching headers on catalogue GET responses | ❌ | No `Cache-Control` or `ETag` headers |
| P-06 | Application-level cache (Redis/ElastiCache) | ❌ | Not present — see REC-06 |
| P-07 | HikariCP pool size tuned | ❌ | See REC-05 |
| P-08 | Full-text search using `tsvector`/`tsquery` | ⚠️ | JPA `Specification` used; adequate for MVP |

---

### 4.4 Observability ✅ / ⚠️ / ❌

| # | Check | Status | Notes |
|---|---|---|---|
| O-01 | Correlation ID propagated through all logs | ✅ | MDC key `correlationId` |
| O-02 | X-Ray distributed tracing enabled | ✅ | App Runner observability config |
| O-03 | CloudWatch alarms: 5xx rate, P99 latency | ✅ | Defined in `cloudwatch.yaml` |
| O-04 | CloudWatch alarms: RDS CPU, connections, storage | ✅ | |
| O-05 | Replica lag alarm | ✅ | |
| O-06 | PagerDuty / Slack SNS notifications | ✅ | Two SNS topics |
| O-07 | Slow query logging (1000ms threshold) | ✅ | RDS parameter group |
| O-08 | SLO/SLI defined and alarm thresholds derived from them | ❌ | See REC-09 |
| O-09 | Outbox FAILED events alerted | ❌ | No alarm on failed outbox rows |
| O-10 | Business metrics (checkout conversion, payment success) | ❌ | See REC-09 |
| O-11 | Structured JSON log format | ⚠️ | See REC-11 |
| O-12 | Actuator endpoints restricted in production | ✅ | Only `health` and `info` public |
| O-13 | CloudWatch Log group encryption | ✅ | KMS key applied |

---

### 4.5 CI/CD & Deployment ✅ / ⚠️ / ❌

| # | Check | Status | Notes |
|---|---|---|---|
| D-01 | No long-lived AWS credentials in CI — OIDC used | ✅ | `id-token: write`, `role-to-assume` |
| D-02 | Docker image immutable tag in ECR | ✅ | SHA-tagged, immutable |
| D-03 | SonarCloud quality gate blocks pipeline | ✅ | `sonar.qualitygate.wait=true` |
| D-04 | Integration tests run against real PostgreSQL (Testcontainers) | ✅ | |
| D-05 | `:stable` tag updated after confirmed deploy | ✅ | `rollback.yml` anchor |
| D-06 | Manual approval gate before production deploy | ⚠️ | GitHub Environment protection rules must be enabled — not verified |
| D-07 | `pom.xml` version promoted from SNAPSHOT to release at tag | ⚠️ | `1.0.0-SNAPSHOT` — must be release-tagged |
| D-08 | Dockerfile OCI labels accurate | ❌ | Placeholder `my-app` / `My Organisation` — see REC-12 |

---

### 4.6 Infrastructure & IaC ✅ / ⚠️ / ❌

| # | Check | Status | Notes |
|---|---|---|---|
| I-01 | All infrastructure defined in CloudFormation | ✅ | App Runner, RDS, ECR, Secrets, IAM, CloudWatch |
| I-02 | Per-environment parameter files | ✅ | `dev/`, `staging/`, `prod/` |
| I-03 | Stack outputs cross-referenced between stacks | ✅ | ARN exports used as parameters |
| I-04 | RDS storage auto-scaling configured | ✅ | Max 500 GB |
| I-05 | VPC connector restricts App Runner egress | ✅ | Private subnets only |
| I-06 | `CidrIp: 0.0.0.0/0` on HTTPS/UDP egress rules | ⚠️ | Should be scoped to VPC endpoint SGs once created |
| I-07 | CloudWatch log retention lifecycle | ⚠️ | Retention days parameterised; ensure non-prod uses shorter period |

---

### Summary: Go / No-Go Assessment

| Gate | Result |
|---|---|
| **Security hardening (rate limiting + swagger block)** | ❌ **BLOCK** |
| **Order idempotency enforcement** | ❌ **BLOCK** |
| **Outbox relay operational** | ❌ **BLOCK** |
| **N+1 order query fixed** | ❌ **BLOCK** |
| **HikariCP pool configured** | ❌ **BLOCK** |
| **`getSessionStore()` deterministic** | ❌ **BLOCK** |
| All Tier 2 recommendations addressed | ⚠️ Recommended before GA |

**Production verdict: NOT ready.** Six blocking items must be resolved. The architectural foundation — DDD structure, security model, IaC, CI/CD pipeline, test coverage, and observability skeleton — is strong and will support a rapid remediation cycle. Estimated remediation effort for all six blockers: **3–5 engineering days**.

---

*Review conducted 2026-09-28. Re-review recommended after all Tier 1 items are closed.*
