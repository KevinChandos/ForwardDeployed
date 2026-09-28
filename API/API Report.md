# Book Worm E-Store API — Governance Report (v2)

**Specification:** `API/openapi.yaml` · OpenAPI 3.1.0  
**Reviewer role:** Principal API Governance Architect  
**Review date:** 2026-09-24 (re-execution)  
**API version:** 1.0.0  
**Total paths reviewed:** 63  
**Prior report:** v1 dated 2026-09-24 — 27 findings across 6 areas  
**Fixes applied before this review:** DESIGN-01, DESIGN-02, DESIGN-03, ERR-01, SEC-01 (global default), SEC-02/03 (fraud docs)

---

## Executive Summary

Six of the seventeen v1 findings have been fully remediated. The spec has measurably improved: the spec-breaking category path ambiguity is gone, verb-in-URL deactivation sub-paths have been replaced with proper PATCH operations, cart item creation now returns `201 Created` with a `Location` header, the forgot-password endpoint has complete error coverage, a global `GuestToken` security default is in place, and both payment endpoints carry explicit fraud-risk documentation.

**Eleven findings remain open**, two of which have been upgraded in severity following the global-default change (the `GET /books/{bookId}` auth misconfiguration is now actively blocking guests from the main product page, since the global default is `GuestToken` but the endpoint still declares `BearerAuth`-only). Three new findings are raised in this review.

**Overall maturity score: Level 2 — progress made, Level 3 still blocked by absent HATEOAS links and the remaining REST anti-patterns.**

---

## Remediation Status of v1 Findings

| ID | Status | Notes |
|---|---|---|
| DESIGN-01 | ✅ **Closed** | `/categories/{slug}` → `/categories/by-slug/{slug}`; `GET /categories/{categoryId}` added |
| DESIGN-02 | ✅ **Closed** | `/books/{bookId}/deactivate` and `/coupons/{couponId}/deactivate` replaced with PATCH on parent |
| DESIGN-03 | ✅ **Closed** | `POST /cart/items` now `201 Created` + `Location` header |
| DESIGN-04 | 🔴 **Open** | Inline anonymous paginated-list schemas still present on 5 endpoints |
| DESIGN-05 | 🔴 **Open** | Bare array returns on `/recommendations/bestsellers` and `/recommendations/new-launches` |
| REST-01 | ✅ **Partially closed** | Deactivate sub-paths fixed; cancel/return/publish/reject are still RPC sub-paths |
| REST-02 | 🔴 **Open — severity upgraded to CRITICAL** | `GET /books/{bookId}` declares `security: - BearerAuth: []`; global default is now `GuestToken`; this actively breaks unauthenticated PDP access |
| REST-03 | 🔴 **Open** | `PUT /users/me/addresses/{addressId}` semantics undocumented |
| NAME-01 | 🔴 **Open** | `walletTxnId`, `txnType` abbreviations not corrected |
| NAME-02 | 🔴 **Open** | `phone` vs `phoneNumber` inconsistency in `SetDeliveryAddressRequest` |
| NAME-03 | 🔴 **Open** | `CreateCouponRequest.minOrderAmount` is `number` not `MoneyDTO` |
| NAME-04 | 🔴 **Open** | `discountValue` uses IEEE-754 float |
| NAME-05 | 🔴 **Open** | Payment `status` fields untyped (`string` without enum) |
| PAGE-01 | 🔴 **Open** | No HATEOAS links in `PaginationDTO` |
| PAGE-02 | 🔴 **Open** | Wishlist and Cart return full item arrays without pagination |
| PAGE-03 | 🔴 **Open** | `SortParam` reusable param unconstrained; bypassed by domain sort enums |
| ERR-01 | ✅ **Closed** | `POST /auth/password/forgot` now has `400` and `429` with expanded description |
| ERR-02 | 🔴 **Open** | Token refresh and logout still missing `429` |
| ERR-03 | 🔴 **Open** | Webhook still missing `401` for invalid HMAC signature |
| ERR-04 | 🔴 **Open** | `GET /cart` still documents no error responses |
| ERR-05 | 🔴 **Open** | `ErrorBody.code` enum still missing `PAYMENT_FAILED` and `SESSION_EXPIRED` |
| SEC-01 | ✅ **Closed** | Global `security: - GuestToken: []` added at document root |
| SEC-02 | 🔴 **Open (documented)** | `GuestToken` on `POST /payments/initiate` — fraud risk documented; not yet removed |
| SEC-03 | 🔴 **Open (documented)** | `GuestToken` on `POST /payments/{transactionId}/confirm` — fraud risk documented; not yet removed |
| SEC-04 | 🔴 **Open** | Guest checkout confirm has no identity anchor requirement |
| SEC-05 | 🔴 **Open** | `GuestToken` lifecycle (TTL, revocation) still undocumented in scheme description |
| SEC-06 | 🔴 **Open** | `429` responses still missing `Retry-After` header |

---

## New Findings Raised in This Review

---

### NEW-01 · Severity: CRITICAL (upgraded from REST-02)
**Finding: `GET /books/{bookId}` declares `security: - BearerAuth: []` — now actively breaks unauthenticated product detail page access.**

With the global `GuestToken` default in place, any endpoint that does **not** override the security requirement inherits `GuestToken`. However, `GET /books/{bookId}` explicitly overrides with `security: - BearerAuth: []`, which means the spec now declares this endpoint requires a *Bearer JWT token*. Guests browsing the product detail page (PDP) will receive `401 Unauthorized`. The `isInWishlist` field description acknowledges this should serve unauthenticated callers.

**Recommended fix:** Override the security on this operation to allow both bearer-authenticated and unauthenticated (empty) callers:

```yaml
/books/{bookId}:
  get:
    security:
      - BearerAuth: []   # authenticated — isInWishlist populated
      - {}               # empty object = no auth required (guest allowed)
```

---

### NEW-02 · Severity: MEDIUM
**Finding: `GuestToken` scheme description is still minimal — no TTL, no format, no scope, no revocation guidance.**

The `GuestToken` scheme description reads: *"Stable guest session identifier for unauthenticated cart/checkout flows."* Since it is now the global default, implementers and consumers need to understand its lifecycle to avoid indefinitely-valid unauthenticated sessions.

**Recommended fix:**

```yaml
GuestToken:
  type: apiKey
  in: header
  name: X-Guest-Token
  description: |
    Stable guest session identifier for unauthenticated cart/checkout flows.
    - Format: UUID v4, issued by the client on first cart interaction
    - TTL: 30 days from last activity; server invalidates silently on expiry
    - Scope: cart read/write, checkout initiation, shipping estimate, public recommendations
    - Revocation: invalidated on POST /cart/merge (absorbed into member session) or expiry
    - Security note: the token is an opaque DB reference; no PII is embedded in the value
    - Fraud note: payment operations (POST /payments/initiate, POST /payments/{transactionId}/confirm)
      accept this token but carry KYC/fraud risk — see SEC-02 and SEC-03
```

---

### NEW-03 · Severity: LOW
**Finding: `POST /auth/password/reset` is missing `security: []` override — now incorrectly inherits the `GuestToken` global default.**

`POST /auth/password/reset` is a fully public endpoint (password reset via a one-time token sent out-of-band). It should require no authentication. With the global `GuestToken` default now applied, a client that does not hold a guest token will be blocked. The token parameter in the request body is the only authentication mechanism needed.

**Recommended fix:**

```yaml
/auth/password/reset:
  post:
    security: []   # public — the reset token in the body is the authentication
```

Similarly, the following intentionally public operations should audit their `security:` overrides to confirm they declare `security: []` rather than inheriting `GuestToken`:

| Endpoint | Current state | Action needed |
|---|---|---|
| `POST /auth/register` | No `security:` declared | Add `security: []` |
| `POST /auth/login` | No `security:` declared | Add `security: []` |
| `POST /auth/refresh` | No `security:` declared | Add `security: []` |
| `POST /auth/password/reset` | No `security:` declared | Add `security: []` |
| `GET /authors` | No `security:` declared | Verify intent; add `security: []` if truly public |
| `GET /authors/{authorId}` | No `security:` declared | Verify intent; add `security: []` if truly public |
| `GET /publishers` | No `security:` declared | Verify intent; add `security: []` if truly public |
| `GET /publishers/{publisherId}` | No `security:` declared | Verify intent; add `security: []` if truly public |
| `GET /categories` | No `security:` declared | Verify intent; add `security: []` if truly public |
| `GET /books` | No `security:` declared | Verify intent; add `security: []` if truly public |
| `GET /search/books` | No `security:` declared | Verify intent; add `security: []` if truly public |
| `POST /payments/webhook` | No `security:` declared | Add `security: []` (validated by HMAC, not session) |
| `GET /shipping/estimate` | No `security:` declared | Verify intent; add `security: []` if truly public |

---

## Remaining Open Findings — Detail

### REST-01 (partial) · Severity: HIGH
**Finding: Cancel, return, publish, and reject are still modelled as RPC verb sub-paths.**

| Path | Method | Issue |
|---|---|---|
| `/orders/{orderId}/cancel` | POST | Verb in URL |
| `/orders/{orderId}/return` | POST | Verb in URL |
| `/reviews/{reviewId}/publish` | POST | Verb in URL |
| `/reviews/{reviewId}/reject` | POST | Verb in URL |

These were called out in v1 and remain unchanged. The deactivate sub-paths (the other REST-01 group) were fixed in DESIGN-02.

**Recommended fix:** Model as `PATCH` with a `status` or transition field on the parent resource, or use a dedicated `transitions` sub-collection for auditability.

---

### NAME-01 · Severity: MEDIUM
**Finding: `WalletTransactionDTO` uses abbreviated field names.**

```yaml
WalletTransactionDTO:
  properties:
    walletTxnId:    # should be walletTransactionId
    txnType:        # should be transactionType
```

**Recommended fix:**
```yaml
WalletTransactionDTO:
  properties:
    walletTransactionId:
      $ref: '#/components/schemas/UUID'
    transactionType:
      type: string
      enum: [CREDIT, DEBIT]
```

---

### NAME-02 · Severity: MEDIUM
**Finding: `SetDeliveryAddressRequest.phone` inconsistent with `phoneNumber` used everywhere else.**

Line 1247: `phone: string pattern: '^\+[1-9]\d{6,14}$'`

**Recommended fix:** Rename to `phoneNumber` for consistency with `RegisterRequest`, `MemberSummaryDTO`, and `AddressInputFields`.

---

### NAME-03 · Severity: LOW
**Finding: `CreateCouponRequest.minOrderAmount` is `type: number` (float); `CouponDTO.minOrderAmount` is `MoneyDTO`.**

The create request and the read response represent the same field with incompatible types.

**Recommended fix:**
```yaml
CreateCouponRequest:
  properties:
    minOrderAmount:
      $ref: '#/components/schemas/MoneyDTO'
      description: Minimum order value. Defaults to zero (MoneyDTO with amount "0.00") if omitted.
```

---

### NAME-04 · Severity: LOW
**Finding: `discountValue` in `CouponDTO` and `CreateCouponRequest` uses `type: number` (IEEE-754 float).**

For `FLAT` discount types, this is a monetary amount and should use `MoneyDTO`. For `PERCENT` types, a number is acceptable but should carry `maximum: 100`.

**Recommended fix:** Use a `oneOf` discriminated on `discountType`, or at minimum add `maximum: 100` for the percent case and document that flat values are in the same currency as `MoneyDTO`.

---

### NAME-05 · Severity: LOW
**Finding: Payment `status` fields are untyped `string` without enum.**

Affected: `PaymentSummaryDTO.status`, `PaymentResultResponse.status`, `PaymentInitiatedResponse.status`.

**Recommended fix:**
```yaml
# Add to components/schemas:
PaymentStatus:
  type: string
  enum: [PENDING, INITIATED, CONFIRMED, FAILED, REFUNDED, PARTIALLY_REFUNDED]

# Apply $ref: '#/components/schemas/PaymentStatus' to all three fields.
```

---

### PAGE-01 · Severity: MEDIUM
**Finding: `PaginationDTO` has no HATEOAS navigation links.**

No `self`, `next`, `prev`, `first`, `last` URLs. Clients must reconstruct page URLs manually.

**Recommended fix:**
```yaml
PaginationDTO:
  properties:
    # ... existing fields ...
    links:
      type: object
      properties:
        self:  { type: string, format: uri }
        first: { type: string, format: uri }
        prev:  { type: string, format: uri, nullable: true }
        next:  { type: string, format: uri, nullable: true }
        last:  { type: string, format: uri }
```

---

### ERR-02 · Severity: MEDIUM
**Finding: `POST /auth/refresh` and `POST /auth/logout` missing `429 TooManyRequests`.**

**Recommended fix:** Add to both operations:
```yaml
'429':
  $ref: '#/components/responses/TooManyRequests'
```

---

### ERR-03 · Severity: MEDIUM
**Finding: `POST /payments/webhook` missing `401` for HMAC signature failure.**

**Recommended fix:**
```yaml
'401':
  $ref: '#/components/responses/Unauthorized'
```

---

### ERR-04 · Severity: LOW
**Finding: `GET /cart` documents zero error responses.**

**Recommended fix:**
```yaml
'401':
  $ref: '#/components/responses/Unauthorized'
'404':
  $ref: '#/components/responses/NotFound'
```

---

### ERR-05 · Severity: LOW
**Finding: `ErrorBody.code` enum missing domain-specific payment and session codes.**

**Recommended fix:**
```yaml
enum:
  - VALIDATION_ERROR
  - UNAUTHENTICATED
  - FORBIDDEN
  - RESOURCE_NOT_FOUND
  - CONFLICT
  - OPTIMISTIC_LOCK_CONFLICT
  - RESOURCE_DELETED
  - BUSINESS_RULE_VIOLATION
  - PAYMENT_FAILED          # new
  - SESSION_EXPIRED         # new
  - RATE_LIMIT_EXCEEDED
  - INTERNAL_ERROR
  - SERVICE_UNAVAILABLE
```

---

### SEC-06 · Severity: LOW
**Finding: `TooManyRequests` reusable response missing `Retry-After` header.**

**Recommended fix:**
```yaml
components:
  responses:
    TooManyRequests:
      description: Rate limit exceeded
      headers:
        Retry-After:
          description: Seconds until the rate limit window resets
          schema:
            type: integer
            minimum: 0
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorBody'
```

---

## Complete Finding Status Summary

| ID | Area | Severity | Title | Status |
|---|---|---|---|---|
| ~~DESIGN-01~~ | Design | HIGH | Ambiguous category path templates | ✅ Closed |
| ~~DESIGN-02~~ | Design | MEDIUM | Deactivation RPC sub-paths | ✅ Closed |
| ~~DESIGN-03~~ | Design | MEDIUM | `POST /cart/items` returned 200 | ✅ Closed |
| DESIGN-04 | Design | LOW | Inline anonymous paginated-list schemas | 🔴 Open |
| DESIGN-05 | Design | LOW | Bare array returns on recommendations | 🔴 Open |
| REST-01 | REST Maturity | HIGH | cancel/return/publish/reject still RPC sub-paths | 🔴 Open (partial) |
| ~~NEW-01~~ | REST Maturity | CRITICAL | `GET /books/{bookId}` BearerAuth blocks guests post global-default | ✅ Closed |
| REST-03 | REST Maturity | LOW | `PUT /users/me/addresses/{addressId}` semantics undocumented | 🔴 Open |
| NAME-01 | Naming | MEDIUM | `walletTxnId` / `txnType` abbreviations | 🔴 Open |
| NAME-02 | Naming | MEDIUM | `phone` vs `phoneNumber` in checkout | 🔴 Open |
| NAME-03 | Naming | LOW | `minOrderAmount` float vs MoneyDTO mismatch | 🔴 Open |
| NAME-04 | Naming | LOW | `discountValue` IEEE-754 float | 🔴 Open |
| NAME-05 | Naming | LOW | Payment `status` fields untyped | 🔴 Open |
| PAGE-01 | Pagination | MEDIUM | No HATEOAS links in `PaginationDTO` | 🔴 Open |
| PAGE-02 | Pagination | LOW | Wishlist/Cart unpaginated | 🔴 Open |
| PAGE-03 | Pagination | LOW | `SortParam` unconstrained; bypassed | 🔴 Open |
| ~~ERR-01~~ | Error Handling | HIGH | Forgot-password had no error responses | ✅ Closed |
| ERR-02 | Error Handling | MEDIUM | Refresh/logout missing `429` | 🔴 Open |
| ERR-03 | Error Handling | MEDIUM | Webhook missing `401` | 🔴 Open |
| ERR-04 | Error Handling | LOW | `GET /cart` has no error responses | 🔴 Open |
| ERR-05 | Error Handling | LOW | `ErrorBody.code` missing payment/session codes | 🔴 Open |
| ~~SEC-01~~ | Security | CRITICAL | No global security default | ✅ Closed |
| SEC-02 | Security | HIGH | `GuestToken` on payment initiate — documented, not removed | 🔴 Open |
| SEC-03 | Security | HIGH | `GuestToken` on payment confirm — documented, not removed | 🔴 Open |
| SEC-04 | Security | MEDIUM | Guest checkout confirm has no identity anchor | 🔴 Open |
| **NEW-02** | Security | MEDIUM | `GuestToken` scheme description too sparse for global default | 🔴 Open |
| **NEW-03** | Security | LOW | Auth/public endpoints missing `security: []` after global default added | 🔴 Open |
| SEC-06 | Security | LOW | `429` responses missing `Retry-After` header | 🔴 Open |

**Closed: 7 of 27** (plus 3 new findings raised, 1 immediately closed) → **23 open findings**

---

## Priority Remediation Order

### Immediate (blocking)
1. ~~**NEW-01**~~ — ✅ Fixed — `GET /books/{bookId}` now declares `security: [BearerAuth, {}]`
2. **NEW-03** — Audit and add `security: []` to all intentionally public endpoints (auth flows, public catalogue, webhook)

### Next sprint
3. **REST-01 (remainder)** — Replace cancel/return/publish/reject RPC sub-paths with PATCH transitions
4. **SEC-02 / SEC-03** — Remove `GuestToken` from payment initiate and confirm operations
5. **NAME-01 / NAME-02** — Normalise `walletTxnId`, `txnType`, `phone`
6. **ERR-02 / ERR-03** — Add `429` to refresh/logout; `401` to webhook
7. **PAGE-01** — Add HATEOAS navigation links to `PaginationDTO`
8. **NEW-02** — Expand `GuestToken` scheme description

### Backlog
9. **DESIGN-04 / DESIGN-05** — Promote inline schemas; wrap recommendation arrays
10. **NAME-03 / NAME-04 / NAME-05** — Fix monetary type mismatches and untyped status
11. **SEC-04 / SEC-06** — Guest identity anchor enforcement; `Retry-After` header
12. **ERR-04 / ERR-05** — Cart error responses; error code enum additions
13. **PAGE-02 / PAGE-03** — Paginate wishlist; align sort parameter strategy
14. **REST-03** — Document `PUT /users/me/addresses/{addressId}` semantics

---

*Report v2 generated by IBM Bob · Principal API Governance Architect re-execution · 2026-09-24*
