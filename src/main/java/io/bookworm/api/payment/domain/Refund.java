package io.bookworm.api.payment.domain;

import io.bookworm.api.checkout.domain.Order;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a refund initiated against a PaymentTransaction.
 * <p>
 * Why: refunds are distinct from payment transactions — a single payment may
 * produce one or more partial refunds. Storing the {@code orderId} directly
 * (denormalised from transaction → order) enables efficient queries like
 * "all refunds for order X" without a join chain.
 * <p>
 * Side effects: the {@code amount} must be positive (DB CHECK). Refund amounts
 * must not collectively exceed the original transaction amount — this invariant
 * is enforced at the application layer, not the DB level. {@code completedAt}
 * is set when the gateway confirms the refund settlement.
 */
@Entity
@Table(schema = "payment", name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Refund extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refund_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID refundId;

    /** The payment transaction being refunded. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_refunds_transaction"))
    private PaymentTransaction transaction;

    /**
     * Denormalised order reference for query efficiency.
     * Duplicates the FK chain transaction → order but avoids a join.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_refunds_order"))
    private Order order;

    /** Refund amount. Must be positive (DB CHECK: amount > 0). */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    /** Reason for the refund (e.g. "Order cancelled by customer"). */
    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    /** Current refund status. DB CHECK enforces allowed values. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status = RefundStatus.INITIATED;

    /** Gateway's own refund reference ID. Populated after initiation. */
    @Column(name = "gateway_refund_id", length = 200)
    private String gatewayRefundId;

    /** Timestamp when this refund was initiated. */
    @Column(name = "initiated_at", nullable = false)
    private OffsetDateTime initiatedAt = OffsetDateTime.now();

    /** Timestamp when the gateway confirmed the refund settlement. */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Refund lifecycle states. */
    public enum RefundStatus {
        INITIATED,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
