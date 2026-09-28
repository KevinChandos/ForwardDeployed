package io.bookworm.api.promotions.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import io.bookworm.api.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Defines the gift/loyalty points earning and redemption policy for a Store.
 * <p>
 * Why: points policies are store-specific configuration that drive the wallet
 * credit calculation when an order is delivered and the redemption cap during
 * checkout. Only one active policy per store is permitted (partial unique index).
 * Storing rates as NUMERIC(8,4) allows fine-grained ratios (e.g. 0.0100 points
 * per rupee) without floating-point imprecision.
 * <p>
 * Side effects: deactivating the current policy ({@code isActive = false}) stops
 * new points accrual immediately. Existing wallet balances are unaffected.
 */
@Entity
@Table(schema = "promotions", name = "gift_point_policies")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GiftPointPolicy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID policyId;

    /** The store this points policy applies to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gift_point_policies_store"))
    private Store store;

    /**
     * Points earned per ₹1 (or base currency unit) spent.
     * DB CHECK: points_per_rupee > 0.
     */
    @Column(name = "points_per_rupee", nullable = false, precision = 8, scale = 4)
    private BigDecimal pointsPerRupee;

    /**
     * Rupee value of a single point at redemption time.
     * DB CHECK: rupees_per_point > 0.
     */
    @Column(name = "rupees_per_point", nullable = false, precision = 8, scale = 4)
    private BigDecimal rupeesPerPoint;

    /**
     * Maximum percentage of the order total payable via points redemption.
     * Range: 0 < value ≤ 100. DB CHECK enforces this.
     */
    @Column(name = "max_redemption_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxRedemptionPercent;

    /**
     * Whether this policy is currently active for the store.
     * Only one active policy per store (partial unique index in DB).
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
