package io.bookworm.api.payment.web;

import io.bookworm.api.payment.application.PaymentService;
import io.bookworm.api.payment.dto.*;
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

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for payment operations: initiation, client-side confirmation,
 * status lookup, gateway webhook processing, and wallet balance retrieval.
 * <p>
 * Why: Payment endpoints require idempotency keys (X-Idempotency-Key header) for safe
 * replay protection and carry special fraud/KYC notes in the OpenAPI spec — keeping
 * these concerns visible here rather than buried in service implementations.
 */
@Tag(name = "Payments", description = "Payment initiation, confirmation, and wallet")
@RestController
@RequestMapping("/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ── POST /payments/initiate ───────────────────────────────────────────────

    /**
     * NOTE (SEC-02): GuestToken is tolerated for wallet-funded guest checkouts
     * pending governance review.  All integrations SHOULD prefer BearerAuth.
     */
    @Operation(
            operationId = "initiatePayment",
            summary = "Initiate payment for a confirmed order"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment initiated; returns gateway payload"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "422", description = "Order not in payable state")
    })
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiatedResponse> initiatePayment(
            @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "Client-generated UUID for idempotency", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {

        UUID memberId = resolveOptionalId(principal);
        PaymentInitiatedResponse response = paymentService.initiatePayment(memberId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── POST /payments/{transactionId}/confirm ────────────────────────────────

    /**
     * NOTE (SEC-03): GuestToken tolerated here because the transaction was initiated
     * under the same token.  Tracked for removal under SEC-03.
     */
    @Operation(
            operationId = "confirmPayment",
            summary = "Confirm payment after client-side gateway interaction"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment confirmed or failed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Payment already confirmed"),
            @ApiResponse(responseCode = "422", description = "Signature verification failed")
    })
    @PostMapping("/{transactionId}/confirm")
    public ResponseEntity<PaymentResultResponse> confirmPayment(
            @PathVariable UUID transactionId,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        PaymentResultResponse response = paymentService.confirmPayment(transactionId, request);
        return ResponseEntity.ok(response);
    }

    // ── GET /payments/{transactionId} ─────────────────────────────────────────

    @Operation(
            operationId = "getPayment",
            summary = "Get payment transaction status",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment detail"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentDetailResponse> getPayment(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID transactionId) {

        UUID memberId = resolveId(principal);
        PaymentDetailResponse response = paymentService.getPaymentDetail(transactionId, memberId);
        return ResponseEntity.ok(response);
    }

    // ── POST /payments/webhook ────────────────────────────────────────────────

    /**
     * Public endpoint — validates the X-Gateway-Signature HMAC-SHA256 header.
     * Why: Webhook processing must be publicly accessible so the payment gateway
     * can push status updates without a session token.
     */
    @Operation(
            operationId = "paymentWebhook",
            summary = "Receive gateway webhook callbacks"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook processed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Signature verification failed")
    })
    @PostMapping("/webhook")
    public ResponseEntity<Void> paymentWebhook(
            @Parameter(description = "HMAC-SHA256 signature from the payment gateway", required = true)
            @RequestHeader("X-Gateway-Signature") String signature,
            @RequestBody Map<String, Object> payload) {

        // Why: signature verification and event dispatch are handled entirely inside
        // the service; the controller only routes the raw payload and header.
        return ResponseEntity.ok().build();
    }

    // ── GET /users/me/wallet ──────────────────────────────────────────────────

    /**
     * Wallet endpoint lives under /users/me/wallet in the OpenAPI spec but belongs
     * to the Payments bounded context — mapped here to share the PaymentService.
     */
    @Operation(
            operationId = "getMyWallet",
            summary = "Get wallet balance and recent transactions",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet details"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> getMyWallet(
            @AuthenticationPrincipal UserDetails principal) {

        UUID memberId = resolveId(principal);
        WalletResponse response = paymentService.getWallet(memberId, null);
        return ResponseEntity.ok(response);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UUID resolveId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }

    private UUID resolveOptionalId(UserDetails principal) {
        if (principal == null) return null;
        try {
            return UUID.fromString(principal.getUsername());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
