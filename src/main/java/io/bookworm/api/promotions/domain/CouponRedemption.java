package io.bookworm.api.promotions.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.checkout.domain.Order;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records a single redemption of a Coupon against an Order.
 * <p>
 * Why: tracking individual redemptions enables per-user coupon limits and
 * provides an audit trail for coupon usage analytics. The unique constraint on
 * {@code (order_id)} prevents a coupon from being applied twice to the same order.
 * <p>
 * Side effects: soft-deleting this row "releases" the coupon when an order is
 * cancelled — the service must also decrement {@link Coupon#getUsedCount()} in
 * the same transaction to keep the count consistent.
 */
@Entity
@Table(
    schema = "promotions",
    name = "coupon_redemptions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_coupon_redemptions_order",
                columnNames = "order_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CouponRedemption extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "redemption_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID redemptionId;

    /** The coupon that was redeemed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_coupon_redemptions_coupon"))
    private Coupon coupon;

    /**
     * The redeeming member. NULL for guest redemptions.
     * Either member or guestToken must be set.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",
                foreignKey = @ForeignKey(name = "fk_coupon_redemptions_member"))
    private Member member;

    /**
     * Guest token for guest redemptions. NULL for member redemptions.
     */
    @Column(name = "guest_token", length = 200)
    private String guestToken;

    /** The order this redemption is linked to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_coupon_redemptions_order"))
    private Order order;

    /** Timestamp when the coupon was applied to the order. */
    @Column(name = "redeemed_at", nullable = false)
    private OffsetDateTime redeemedAt = OffsetDateTime.now();
}
