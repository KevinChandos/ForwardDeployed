package io.bookworm.api.payment.application;

import io.bookworm.api.payment.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for payment gateway integration, transaction processing, and wallet balance operations.
 * <p>
 * Why: Encapsulates initiation of payment gateways (e.g. Razorpay, UPI, Card), webhook/callback confirmation,
 * idempotency safety, transaction lookup, and wallet ledger management.
 */
public interface PaymentService {

    /**
     * Initiates a payment session for an order, creating a transaction record and generating gateway redirect URL.
     */
    PaymentInitiatedResponse initiatePayment(UUID memberId, InitiatePaymentRequest request, String idempotencyKey);

    /**
     * Confirms the payment status after gateway callback or webhook signature verification.
     */
    PaymentResultResponse confirmPayment(UUID transactionId, ConfirmPaymentRequest request);

    /**
     * Retrieves details of a specific payment transaction.
     */
    PaymentDetailResponse getPaymentDetail(UUID transactionId, UUID memberId);

    /**
     * Retrieves the member's wallet account balance and paginated ledger history.
     */
    WalletResponse getWallet(UUID memberId, Pageable pageable);
}
