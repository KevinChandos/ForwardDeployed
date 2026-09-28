# Interaction Summary

## Performed By
- `KevinChandos (AzureAD)`

## Initial Prompt
> Now update the @Architecture\API\Design.md so that it matches the updated @API\openapi.yaml.

## Objective
Synchronise `Architecture/API Design.md` (v1.0) with every governance change applied to `API/openapi.yaml` in the previous session, so the human-readable design document is the single consistent source of truth alongside the spec.

## Repository Investigation
- Read `Architecture/API Design.md` in full (lines 1–1894) to locate every section affected by the six governance changes
- Confirmed all stale references (old path names, old status codes, missing error tables) by cross-checking against the updated `openapi.yaml`

## Actions Taken

### Global Conventions → Authentication (line 19)
- Added paragraph documenting the global `GuestToken` security default; explains override pattern for `BearerAuth` and `security: []`

### Domain 1 — POST /auth/password/forgot (line 211)
- Expanded `202` description to explain the constant-response anti-enumeration rationale
- Added note that reset link/OTP is dispatched out-of-band
- Added **Error Codes** table: `VALIDATION_ERROR` (malformed body) + `RATE_LIMIT_EXCEEDED`
- Added `security: []` note in the endpoint heading

### Domain 5 — Categories (lines 553–600)
- Renamed `GET /categories/{slug}` → `GET /categories/by-slug/{slug}`
- Added governance note (DESIGN-01) explaining the router ambiguity that drove the rename
- Added `Path Params` table with slug pattern constraint
- Added new `GET /categories/{categoryId}` entry with response DTO and error codes

### Domain 6 — Books (line 749)
- Replaced `PATCH /books/{bookId}/deactivate` (bodyless, `204`) with `PATCH /books/{bookId}`
- Added governance note (DESIGN-02)
- Added `BookUpdateStatusRequest` DTO table (`isActive`, `version`)
- Changed response to `BookDetailResponse` `200 OK`
- Added error codes: `RESOURCE_NOT_FOUND`, `OPTIMISTIC_LOCK_CONFLICT`

### Domain 9 — Cart (line 923)
- Added governance note (DESIGN-03) explaining the `200 → 201` change
- Changed response code from `200 OK` to `201 Created`
- Added `Response Headers` table documenting the `Location` header

### Domain 11 — Payments (line 1242)
- Extended domain-level security callout with fraud/KYC risk paragraph referencing SEC-02 and SEC-03
- Added `⚠ Fraud risk` note directly under `POST /payments/initiate`
- Added `⚠ Fraud risk` note directly under `POST /payments/{transactionId}/confirm`

### Domain 14 — Coupons (line 1637)
- Replaced `PATCH /coupons/{couponId}/deactivate` (bodyless, `204`) with `PATCH /coupons/{couponId}`
- Added governance note (DESIGN-02)
- Added `CouponUpdateStatusRequest` DTO table (`isActive`, `version`)
- Changed response to `CouponDTO` `200 OK`
- Added error codes: `RESOURCE_NOT_FOUND`, `OPTIMISTIC_LOCK_CONFLICT`

### URI Summary table (lines 1835–1884)
- `GET /categories/{slug}` → `GET /categories/by-slug/{slug}`
- Added new row `GET /categories/{categoryId}`
- `PATCH /books/{bookId}/deactivate` → `PATCH /books/{bookId}`
- `PATCH /coupons/{couponId}/deactivate` → `PATCH /coupons/{couponId}`

### Document version footer
- Bumped to `1.1`; added change summary referencing findings DESIGN-01, DESIGN-02, DESIGN-03, ERR-01, SEC-02, SEC-03

## Validation
- Grepped `Architecture/API Design.md` for stale path patterns (`/deactivate`, `/categories/{slug}`, `200 OK.*cart`, `204 No Content.*deactivat`)
- All four matches are inside governance-note blockquotes explaining the *old* paths — no live endpoint entries reference removed paths

## Models Used
- claude-sonnet-4-5 (single model throughout)

## Outputs
- **Modified:** `Architecture/API Design.md` — 11 distinct change sets; version bumped 1.0 → 1.1
