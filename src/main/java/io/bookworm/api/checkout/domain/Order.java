package io.bookworm.api.checkout.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.common.domain.AuditableEntity;
import io.bookworm.api.promotions.domain.Coupon;
import io.bookworm.api.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable order aggregate root created when a checkout session is confirmed.
 * <p>
 * Why: orders are the financial record of a purchase. Once confirmed, the monetary
 * fields and delivery address are immutable snapshots — they must not change even
 * if the underlying catalogue data changes. The order status drives all downstream
 * workflows: payment, shipping, returns, refunds.
 * <p>
 * Side effects: all monetary fields use NUMERIC(14,2). Coupon code is snapshotted
 * in {@code couponCodeSnapshot} so the displayed code survives even if the Coupon
 * row is soft-deleted. Optimistic locking applies to status transitions; the client
 * must supply {@code version} for cancellation.
 */
@Entity
@Table(schema = "ordering", name = "orders")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID orderId;

    /**
     * The ordering member. NULL for guest orders.
     * Either memberId or guestEmail must be non-null (DB CHECK).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",
                foreignKey = @ForeignKey(name = "fk_orders_member"))
    private Member member;

    /** Email for guest order confirmations. NULL for member orders. */
    @Column(name = "guest_email", length = 320)
    private String guestEmail;

    /** The store this order was placed through. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_orders_store"))
    private Store store;

    /**
     * Order lifecycle status. DB CHECK enforces the full state machine alphabet.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    /** Sum of all order line subtotals. */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** subtotal + taxAmount + shippingAmount − discountAmount. DB CHECK: >= 0. */
    @Column(name = "grand_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    /** Applied coupon reference (nullable; set when a coupon was used). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id",
                foreignKey = @ForeignKey(name = "fk_orders_coupon"))
    private Coupon coupon;

    /**
     * Snapshot of the coupon code at order time. Survives coupon soft-delete.
     */
    @Column(name = "coupon_code_snapshot", length = 50)
    private String couponCodeSnapshot;

    /** Amount debited from the member's wallet during checkout. */
    @Column(name = "wallet_debit_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal walletDebitAmount = BigDecimal.ZERO;

    @Column(name = "placed_at")
    private OffsetDateTime placedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /** Immutable snapshot of the delivery address at order confirmation time. */
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrderDeliveryAddress deliveryAddress;

    /** Individual line items composing this order. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderLine> lines = new ArrayList<>();

    /** Return requests initiated against this order. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnRequest> returnRequests = new ArrayList<>();

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Full order lifecycle as defined in the OpenAPI spec and DB CHECK. */
    public enum OrderStatus {
        PENDING_PAYMENT,
        AWAITING_PAYMENT,
        CONFIRMED,
        PROCESSING,
        DISPATCHED,
        DELIVERED,
        CANCELLED,
        RETURN_REQUESTED,
        RETURN_APPROVED,
        RETURN_REJECTED,
        RETURN_IN_TRANSIT,
        RETURN_RECEIVED,
        REFUNDED
    }
}
