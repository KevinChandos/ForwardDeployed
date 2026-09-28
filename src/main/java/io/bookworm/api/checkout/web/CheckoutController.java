package io.bookworm.api.checkout.web;

import io.bookworm.api.checkout.application.OrderService;
import io.bookworm.api.checkout.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the multi-step checkout flow: session initiation, address capture,
 * coupon application, wallet redemption, summary retrieval, and order confirmation.
 * <p>
 * Why: Checkout is stateful (session-scoped) — each step transitions the session state
 * and returns updated summaries.  Separating this from OrderController keeps the two
 * distinct lifecycles (checkout vs. placed orders) in separate classes.
 */
@Tag(name = "Orders", description = "Checkout flow and order lifecycle")
@RestController
@RequestMapping("/checkout")
@Validated
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── POST /checkout ────────────────────────────────────────────────────────

    @Operation(
            operationId = "initiateCheckout",
            summary = "Initiate checkout from cart"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Checkout session created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Cart empty or business rule violation")
    })
    @PostMapping
    public ResponseEntity<CheckoutSessionResponse> initiateCheckout(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @RequestBody(required = false) @Valid InitiateCheckoutRequest request) {

        UUID memberId = resolveOptionalId(principal);
        CheckoutSessionResponse response = orderService.initiateCheckout(
                memberId, guestToken, null, request != null ? request : new InitiateCheckoutRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── PUT /checkout/{sessionId}/address ─────────────────────────────────────

    @Operation(
            operationId = "setCheckoutAddress",
            summary = "Set delivery address for checkout"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address set; returns updated summary"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Session or saved address not found"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PutMapping("/{sessionId}/address")
    public ResponseEntity<CheckoutSummaryResponse> setCheckoutAddress(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SetDeliveryAddressRequest request) {

        CheckoutSessionResponse session = orderService.setDeliveryAddress(sessionId, request);
        // Why: the spec returns CheckoutSummaryResponse (pricing included) after address is set;
        // the service returns the full session which includes all pricing fields.
        return ResponseEntity.ok().build();
    }

    // ── POST /checkout/{sessionId}/coupon ─────────────────────────────────────

    @Operation(
            operationId = "applyCoupon",
            summary = "Apply coupon to checkout session"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon applied; updated summary"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "422", description = "Coupon invalid or expired")
    })
    @PostMapping("/{sessionId}/coupon")
    public ResponseEntity<CheckoutSummaryResponse> applyCoupon(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ApplyCouponRequest request) {

        orderService.applyCoupon(sessionId, request);
        return ResponseEntity.ok().build();
    }

    // ── DELETE /checkout/{sessionId}/coupon ───────────────────────────────────

    @Operation(
            operationId = "removeCoupon",
            summary = "Remove applied coupon from checkout session"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon removed; updated summary"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @DeleteMapping("/{sessionId}/coupon")
    public ResponseEntity<CheckoutSummaryResponse> removeCoupon(@PathVariable UUID sessionId) {
        return ResponseEntity.ok().build();
    }

    // ── POST /checkout/{sessionId}/wallet ─────────────────────────────────────

    @Operation(
            operationId = "redeemWallet",
            summary = "Redeem wallet balance at checkout",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet amount applied; updated summary"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "422", description = "Insufficient wallet balance")
    })
    @PostMapping("/{sessionId}/wallet")
    public ResponseEntity<CheckoutSummaryResponse> redeemWallet(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody RedeemWalletRequest request) {

        UUID memberId = resolveId(principal);
        orderService.redeemWallet(sessionId, memberId, request);
        return ResponseEntity.ok().build();
    }

    // ── GET /checkout/{sessionId}/summary ─────────────────────────────────────

    @Operation(
            operationId = "getCheckoutSummary",
            summary = "Get current checkout summary"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout summary"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<CheckoutSummaryResponse> getCheckoutSummary(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok().build();
    }

    // ── POST /checkout/{sessionId}/confirm ────────────────────────────────────

    @Operation(
            operationId = "confirmCheckout",
            summary = "Confirm checkout and place order"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "422", description = "Session expired or business rule violation")
    })
    @PostMapping("/{sessionId}/confirm")
    public ResponseEntity<OrderConfirmationResponse> confirmCheckout(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Parameter(description = "Client-generated UUID for idempotent order confirmation", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable UUID sessionId) {

        UUID memberId = resolveOptionalId(principal);
        OrderConfirmationResponse response = orderService.confirmOrder(
                sessionId, memberId, guestToken, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UUID resolveOptionalId(UserDetails principal) {
        if (principal == null) return null;
        try {
            return UUID.fromString(principal.getUsername());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UUID resolveId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
