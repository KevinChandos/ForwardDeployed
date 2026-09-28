package io.bookworm.api.checkout.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.cart.domain.Cart;
import io.bookworm.api.common.domain.AuditableEntity;
import io.bookworm.api.promotions.domain.Coupon;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an in-progress checkout flow for a Cart.
 * <p>
 * Why: checkout is a multi-step state machine (CREATED → ADDRESS_SET →
 * PRICING_APPLIED → CONFIRMED → COMPLETED / EXPIRED). Using a dedicated entity
 * decouples the transient checkout state from the immutable Order entity that is
 * only created on confirmation. Expired sessions are cleaned up by a scheduler.
 * <p>
 * Side effects: {@code walletDebitAmount}, subtotal, tax, shipping, discount, and
 * grandTotal are computed and stored during the PRICING_APPLIED step. NULL values
 * mean pricing has not yet been calculated. The {@code coupon} FK is set when the
 * member applies a coupon code; it is validated again at confirmation time.
 */
@Entity
@Table(schema = "checkout", name = "checkout_sessions")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CheckoutSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID sessionId;

    /** The cart being checked out. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_checkout_sessions_cart"))
    private Cart cart;

    /**
     * Authenticated member checking out. NULL for guest checkouts.
     * Either member or guestToken must be non-null (DB CHECK).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",
                foreignKey = @ForeignKey(name = "fk_checkout_sessions_member"))
    private Member member;

    /** Guest session token. NULL for authenticated member checkouts. */
    @Column(name = "guest_token", length = 200)
    private String guestToken;

    /** Current step in the checkout state machine. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CheckoutStatus status = CheckoutStatus.CREATED;

    /** Hard expiry for the session; abandoned sessions past this time are EXPIRED. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Applied coupon (validated at PRICING_APPLIED step). Nullable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id",
                foreignKey = @ForeignKey(name = "fk_checkout_sessions_coupon"))
    private Coupon coupon;

    /** Wallet amount the member elected to redeem. 0 by default. */
    @Column(name = "wallet_debit_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal walletDebitAmount = BigDecimal.ZERO;

    /** Computed during PRICING_APPLIED step. NULL until then. */
    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "shipping_amount", precision = 14, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "discount_amount", precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "grand_total", precision = 14, scale = 2)
    private BigDecimal grandTotal;

    /** Currency for all monetary fields in this session. */
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Checkout state machine steps. */
    public enum CheckoutStatus {
        CREATED,
        ADDRESS_SET,
        PRICING_APPLIED,
        CONFIRMED,
        COMPLETED,
        EXPIRED
    }
}
