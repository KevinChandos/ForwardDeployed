package io.bookworm.api.promotions.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import io.bookworm.api.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A promotional discount coupon scoped to a specific Store.
 * <p>
 * Why: coupons are store-scoped so that different stores can run independent
 * promotions. The {@code code} is normalised to uppercase and must be unique
 * per store among active coupons. The {@code usedCount} field is incremented
 * atomically (via UPDATE … SET used_count = used_count + 1 WHERE …) to prevent
 * race conditions on high-traffic promotions.
 * <p>
 * Side effects: {@code isActive = false} immediately disables the coupon.
 * {@code maxUses = NULL} means unlimited usage. Setting {@code expiresAt} to
 * a past timestamp effectively deactivates the coupon at the service layer check.
 */
@Entity
@Table(
    schema = "promotions",
    name = "coupons",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_coupons_store_code",
                columnNames = {"store_id", "code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Coupon extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "coupon_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID couponId;

    /** The store this coupon belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_coupons_store"))
    private Store store;

    /**
     * Uppercase alphanumeric code that customers enter at checkout.
     * Unique per store among active coupons.
     */
    @Column(name = "code", nullable = false, length = 50)
    @ToString.Include
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * FLAT = fixed amount off; PERCENT = percentage off.
     * DB CHECK enforces allowed values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType;

    /**
     * For FLAT: the exact amount to deduct.
     * For PERCENT: the percentage (0 < value ≤ 100).
     * DB CHECK: discount_value > 0.
     */
    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountValue;

    /** Minimum order subtotal required to apply this coupon. Default 0. */
    @Column(name = "min_order_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** Maximum number of times this coupon can be redeemed. NULL = unlimited. */
    @Column(name = "max_uses")
    private Integer maxUses;

    /**
     * Running redemption count. Incremented atomically on each successful redemption.
     * DB CHECK: used_count >= 0 AND (max_uses IS NULL OR used_count <= max_uses).
     */
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    /** Absolute expiry timestamp. NULL means the coupon never expires. */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /** Whether the coupon is currently active. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Discount calculation strategy. */
    public enum DiscountType {
        FLAT,
        PERCENT
    }
}
