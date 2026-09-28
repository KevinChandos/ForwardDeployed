package io.bookworm.api.payment.domain;

import io.bookworm.api.checkout.domain.Order;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The primary payment transaction entity representing a payment attempt for an Order.
 * <p>
 * Why: payment transactions are the source of truth for financial state. A single
 * order may have one transaction with multiple {@link PaymentAttempt} rows if the
 * gateway is retried. Card details are stored in masked/partial form only —
 * full card data must never be persisted (PCI DSS compliance).
 * <p>
 * Side effects: status transitions are driven by gateway webhooks and confirmation
 * callbacks. {@code confirmedAt} and {@code failedAt} are set once and must not
 * be overwritten. Optimistic locking prevents concurrent webhook handlers from
 * corrupting the status.
 */
@Entity
@Table(schema = "payment", name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID transactionId;

    /** The order this payment is for. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_payment_txn_order"))
    private Order order;

    /** Gateway's own transaction reference (e.g. Razorpay order_id). */
    @Column(name = "gateway_id", length = 200)
    private String gatewayId;

    /** Name of the payment gateway (e.g. "RAZORPAY", "STRIPE"). */
    @Column(name = "gateway_name", length = 100)
    private String gatewayName;

    /**
     * Payment method used. DB CHECK enforces allowed values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Total payment amount. DB CHECK: amount > 0.
     * Stored as NUMERIC(14,2).
     */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    /** Current transaction status. DB CHECK enforces the state machine. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.INITIATED;

    /**
     * Masked card number (last 4 digits only, e.g. "**** 4242").
     * PCI DSS: never store full card number.
     */
    @Column(name = "masked_card_number", length = 20)
    private String maskedCardNumber;

    @Column(name = "cardholder_name", length = 200)
    private String cardholderName;

    /**
     * Card expiry month (1–12). DB CHECK: card_expiry_month BETWEEN 1 AND 12.
     */
    @Column(name = "card_expiry_month", columnDefinition = "SMALLINT")
    private Short cardExpiryMonth;

    /** 4-digit expiry year. */
    @Column(name = "card_expiry_year", columnDefinition = "SMALLINT")
    private Short cardExpiryYear;

    /** Masked UPI virtual payment address (VPA). */
    @Column(name = "upi_id", length = 100)
    private String upiId;

    /** Timestamp when payment was confirmed by the gateway. Set once; never overwritten. */
    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    /** Timestamp when payment definitively failed. Set once; never overwritten. */
    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    /** Individual gateway attempt records (retries). */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentAttempt> attempts = new ArrayList<>();

    /** Refund records associated with this transaction. */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Refund> refunds = new ArrayList<>();

    // ── Enums ───────────────────────────────────────────────────────────────

    /** Payment method options. */
    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        UPI,
        WALLET
    }

    /** Transaction lifecycle states. */
    public enum TransactionStatus {
        INITIATED,
        PENDING,
        CONFIRMED,
        FAILED,
        TIMED_OUT,
        REFUND_PENDING,
        REFUNDED,
        REFUND_FAILED
    }
}
