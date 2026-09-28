package io.bookworm.api.store.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Operational policy for a Store — return window, free delivery threshold, etc.
 * <p>
 * Why: store policies are versioned rows rather than columns on Store, so that
 * policy history is preserved. Only one active policy per store should exist at
 * a time; the unique partial index enforces this at the DB level.
 * <p>
 * Side effects: if {@code freeDeliveryThreshold} is null, no free delivery tier
 * applies and shipping cost is always calculated from the delivery thresholds.
 */
@Entity
@Table(schema = "store", name = "store_policies")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StorePolicy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID policyId;

    /** The store this policy governs. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_store_policies_store"))
    private Store store;

    /** Number of days within which a return can be requested after delivery. */
    @Column(name = "return_window_days", nullable = false)
    private Integer returnWindowDays = 7;

    /**
     * Order subtotal above which shipping is free. NULL disables free delivery tier.
     * Stored as NUMERIC(14,2).
     */
    @Column(name = "free_delivery_threshold", precision = 14, scale = 2)
    private BigDecimal freeDeliveryThreshold;

    /** Currency for the threshold amount (ISO-4217). */
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    /**
     * Whether this is the currently active policy for the store.
     * Only one active policy per store is permitted (partial unique index in DB).
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
