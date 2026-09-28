package io.bookworm.api.payment.application;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.checkout.domain.Order;
import io.bookworm.api.checkout.infrastructure.OrderRepository;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.dto.PaginationDTO;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.payment.domain.PaymentAttempt;
import io.bookworm.api.payment.domain.PaymentTransaction;
import io.bookworm.api.payment.domain.WalletAccount;
import io.bookworm.api.payment.domain.WalletTransaction;
import io.bookworm.api.payment.dto.*;
import io.bookworm.api.payment.infrastructure.PaymentAttemptRepository;
import io.bookworm.api.payment.infrastructure.PaymentTransactionRepository;
import io.bookworm.api.payment.infrastructure.WalletAccountRepository;
import io.bookworm.api.payment.infrastructure.WalletTransactionRepository;
import io.bookworm.api.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Payment & Wallet bounded context.
 * <p>
 * Why: Orchestrates payment transaction initiation, gateway payment URL construction, callback settlement,
 * payment attempt audit trails, and member wallet ledger operations.
 * Side effects: Mutates PaymentTransaction, PaymentAttempt, Order, WalletAccount, and WalletTransaction entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentInitiatedResponse initiatePayment(UUID memberId, InitiatePaymentRequest request, String idempotencyKey) {
        log.info("Initiating payment for orderId: {}, method: {}", request.getOrderId(), request.getPaymentMethod());

        if (request.getOrderId() == null) {
            throw new BusinessRuleException("INVALID_ORDER", "orderId is required");
        }

        Order order = orderRepository.findActive(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));

        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT &&
            order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT &&
            order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new BusinessRuleException("ORDER_NOT_PAYABLE",
                    "Order in state " + order.getStatus() + " cannot accept payments");
        }

        PaymentTransaction.PaymentMethod method;
        try {
            method = PaymentTransaction.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_PAYMENT_METHOD", "Unknown payment method: " + request.getPaymentMethod());
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setPaymentMethod(method);
        transaction.setAmount(order.getGrandTotal());
        transaction.setCurrency(order.getCurrency() != null ? order.getCurrency() : "INR");
        transaction.setStatus(PaymentTransaction.TransactionStatus.INITIATED);
        transaction.setGatewayName("RAZORPAY");
        transaction.setGatewayId("order_mock_" + UUID.randomUUID().toString().substring(0, 8));

        PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setTransaction(savedTxn);
        attempt.setAttemptedAt(OffsetDateTime.now());
        attempt.setGatewayStatus("CREATED");
        paymentAttemptRepository.save(attempt);

        String paymentUrl = "https://checkout.razorpay.com/v1/checkout.js?order_id=" + savedTxn.getGatewayId() +
                "&callback_url=" + (request.getReturnUrl() != null ? request.getReturnUrl() : "");

        log.info("Payment transaction {} initiated for order {}", savedTxn.getTransactionId(), order.getOrderId());
        return paymentMapper.toPaymentInitiatedResponse(savedTxn, paymentUrl);
    }

    @Override
    @Transactional
    public PaymentResultResponse confirmPayment(UUID transactionId, ConfirmPaymentRequest request) {
        log.info("Confirming payment transaction: {}, status: {}", transactionId, request.getStatus());

        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", transactionId));

        if (transaction.getStatus() == PaymentTransaction.TransactionStatus.CONFIRMED) {
            log.info("Payment transaction {} is already CONFIRMED (idempotent)", transactionId);
            return paymentMapper.toPaymentResultResponse(transaction);
        }

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(request.getStatus()) || "CAPTURED".equalsIgnoreCase(request.getStatus());

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setTransaction(transaction);
        attempt.setAttemptedAt(OffsetDateTime.now());
        attempt.setGatewayStatus(request.getStatus());

        if (isSuccess) {
            transaction.setStatus(PaymentTransaction.TransactionStatus.CONFIRMED);
            transaction.setConfirmedAt(OffsetDateTime.now());
            if (request.getGatewayPaymentId() != null) {
                transaction.setGatewayId(request.getGatewayPaymentId());
            }

            Order order = transaction.getOrder();
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setConfirmedAt(OffsetDateTime.now());
            orderRepository.save(order);
            log.info("Payment transaction {} confirmed, order {} updated", transactionId, order.getOrderId());
        } else {
            transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
            transaction.setFailedAt(OffsetDateTime.now());
            attempt.setFailureReason("Payment rejected by gateway or user cancelled");
            log.warn("Payment transaction {} failed", transactionId);
        }

        paymentAttemptRepository.save(attempt);
        PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);

        return paymentMapper.toPaymentResultResponse(savedTxn);
    }

    @Override
    public PaymentDetailResponse getPaymentDetail(UUID transactionId, UUID memberId) {
        log.info("Fetching payment details for transactionId: {}", transactionId);
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", transactionId));

        if (memberId != null && transaction.getOrder().getMember() != null &&
            !transaction.getOrder().getMember().getMemberId().equals(memberId)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Access to this payment record is forbidden");
        }

        return paymentMapper.toPaymentDetailResponse(transaction);
    }

    @Override
    public WalletResponse getWallet(UUID memberId, Pageable pageable) {
        log.info("Fetching wallet for memberId: {}", memberId);
        WalletAccount wallet = walletAccountRepository.findActiveByMemberId(memberId)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
                    WalletAccount newWallet = new WalletAccount();
                    newWallet.setMember(member);
                    newWallet.setBalance(BigDecimal.ZERO);
                    newWallet.setCurrency("INR");
                    return walletAccountRepository.save(newWallet);
                });

        Page<WalletTransaction> page = walletTransactionRepository.findActiveByWalletId(wallet.getWalletId(), pageable);
        List<WalletTransactionDTO> transactionDTOs = paymentMapper.toWalletTransactionDTOList(page.getContent());

        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        MoneyDTO rupeeValue = MoneyDTO.builder()
                .amount(wallet.getBalance().toPlainString())
                .currency(wallet.getCurrency() != null ? wallet.getCurrency() : "INR")
                .build();

        return paymentMapper.toWalletResponse(wallet, rupeeValue, transactionDTOs, pagination);
    }
}
