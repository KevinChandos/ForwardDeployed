package io.bookworm.api.store.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Defines a shipping cost tier for a Store based on order subtotal.
 * <p>
 * Why: shipping costs are tiered — orders above a threshold pay a reduced or
 * zero shipping fee. Multiple rows per store define a step-function pricing
 * ladder. The service layer selects the row where
 * {@code min_order_amount <= order_subtotal} and uses the highest matching tier.
 * <p>
 * Side effects: {@code shippingCost = 0} means free shipping for that tier.
 * Both {@code minOrderAmount} and {@code shippingCost} must be >= 0 per DB CHECK.
 */
@Entity
@Table(schema = "store", name = "delivery_thresholds")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DeliveryThreshold extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "threshold_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID thresholdId;

    /** The store this threshold belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_delivery_thresholds_store"))
    private Store store;

    /**
     * Minimum order subtotal (inclusive) to qualify for this shipping cost tier.
     * DB CHECK: min_order_amount >= 0.
     */
    @Column(name = "min_order_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal minOrderAmount;

    /**
     * Shipping cost charged when this tier applies. 0 means free shipping.
     * DB CHECK: shipping_cost >= 0.
     */
    @Column(name = "shipping_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingCost;

    /** Currency for the threshold and cost values (ISO-4217). */
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";
}
