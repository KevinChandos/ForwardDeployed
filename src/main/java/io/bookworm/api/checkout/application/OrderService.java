package io.bookworm.api.checkout.application;

import io.bookworm.api.checkout.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for checkout and order lifecycle management.
 * <p>
 * Why: Orchestrates multi-step checkout workflows (initiation, address capture, coupon application,
 * wallet redemption, confirmation) and order operations (order detail lookup, order history paging,
 * cancellation, and return requests).
 */
public interface OrderService {

    // ── Checkout Flow ──────────────────────────────────────────────────────────
    CheckoutSessionResponse initiateCheckout(UUID memberId, String guestToken, UUID storeId, InitiateCheckoutRequest request);

    CheckoutSessionResponse setDeliveryAddress(UUID sessionId, SetDeliveryAddressRequest request);

    CheckoutSessionResponse applyCoupon(UUID sessionId, ApplyCouponRequest request);

    CheckoutSessionResponse redeemWallet(UUID sessionId, UUID memberId, RedeemWalletRequest request);

    OrderConfirmationResponse confirmOrder(UUID sessionId, UUID memberId, String guestToken, String idempotencyKey);

    // ── Order Management ───────────────────────────────────────────────────────
    OrderDetailResponse getOrderById(UUID orderId, UUID memberId);

    OrderListResponse getMemberOrders(UUID memberId, Pageable pageable);

    void cancelOrder(UUID orderId, UUID memberId, CancelOrderRequest request);

    ReturnRequestDTO requestReturn(UUID orderId, UUID memberId, ReturnRequestBody request);
}
