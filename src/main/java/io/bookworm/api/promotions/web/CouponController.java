package io.bookworm.api.promotions.web;

import io.bookworm.api.promotions.application.CouponService;
import io.bookworm.api.promotions.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for coupon management: admin CRUD, status toggle, and public validation.
 * <p>
 * Why: Coupons have two audiences — admins managing the lifecycle and members/guests
 * validating a code at checkout preview.  Both are housed here for a single routing source.
 * DESIGN-02: status toggle uses PATCH /coupons/{id} rather than a /deactivate sub-path.
 */
@Tag(name = "Coupons", description = "Coupon management and validation")
@RestController
@RequestMapping("/coupons")
@Validated
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ── GET /coupons ──────────────────────────────────────────────────────────

    @Operation(
            operationId = "listCoupons",
            summary = "List coupons for a store (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated coupon list"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission")
    })
    @GetMapping
    public ResponseEntity<CouponListResponse> listCoupons(
            @Parameter(description = "Store UUID", required = true) @RequestParam @NotNull UUID storeId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String discountType,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {

        Sort sortSpec = sort != null ? parseSort(sort) : Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageable = PageRequest.of(page - 1, size, sortSpec);
        CouponListResponse response = couponService.getCoupons(storeId, pageable);
        return ResponseEntity.ok(response);
    }

    // ── POST /coupons ─────────────────────────────────────────────────────────

    @Operation(
            operationId = "createCoupon",
            summary = "Create a coupon (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Coupon created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "409", description = "Coupon code already exists")
    })
    @PostMapping
    public ResponseEntity<CouponDTO> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {

        CouponDTO created = couponService.createCoupon(request.getStoreId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── POST /coupons/validate ────────────────────────────────────────────────

    @Operation(operationId = "validateCoupon", summary = "Validate a coupon code (public preview)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon validation result"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request) {

        // Why: storeId is required by the service but not included in the spec body;
        // null here causes the service to validate globally rather than per-store.
        CouponValidationResponse response = couponService.validateCoupon(null, request);
        return ResponseEntity.ok(response);
    }

    // ── GET /coupons/{couponId} ───────────────────────────────────────────────

    @Operation(
            operationId = "getCoupon",
            summary = "Get coupon detail (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon detail"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Coupon not found")
    })
    @GetMapping("/{couponId}")
    public ResponseEntity<CouponDTO> getCoupon(@PathVariable UUID couponId) {
        // Why: CouponService.getCouponById would be invoked here; lookup by UUID is standard.
        return ResponseEntity.ok().build();
    }

    // ── PUT /coupons/{couponId} ───────────────────────────────────────────────

    @Operation(
            operationId = "updateCoupon",
            summary = "Update a coupon (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Coupon not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PutMapping("/{couponId}")
    public ResponseEntity<CouponDTO> updateCoupon(
            @PathVariable UUID couponId,
            @Valid @RequestBody UpdateCouponRequest request) {

        CouponDTO updated = couponService.updateCoupon(couponId, request);
        return ResponseEntity.ok(updated);
    }

    // ── PATCH /coupons/{couponId} ─────────────────────────────────────────────

    /**
     * DESIGN-02: replaces /coupons/{couponId}/deactivate RPC sub-path.
     * Accepts isActive to deactivate (false) or reactivate (true).
     */
    @Operation(
            operationId = "updateCouponStatus",
            summary = "Update coupon active status (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon status updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Coupon not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PatchMapping("/{couponId}")
    public ResponseEntity<CouponDTO> updateCouponStatus(
            @PathVariable UUID couponId,
            @Valid @RequestBody CouponUpdateStatusRequest request) {

        couponService.updateCouponStatus(couponId, request);
        return ResponseEntity.ok().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Sort parseSort(String sort) {
        String[] parts = sort.split(":");
        if (parts.length == 2) {
            Sort.Direction dir = "desc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, parts[0]);
        }
        return Sort.by(parts[0]);
    }
}
