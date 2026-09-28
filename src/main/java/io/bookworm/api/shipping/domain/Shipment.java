package io.bookworm.api.shipping.domain;

import io.bookworm.api.checkout.domain.Order;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the physical or digital shipment for a confirmed Order.
 * <p>
 * Why: shipment tracking data is maintained as a separate entity from Order to
 * keep the order aggregate clean and allow the shipping subsystem to evolve
 * independently. One active shipment per order is enforced by a partial unique index.
 * <p>
 * Side effects: status transitions from CREATED → DELIVERED are driven by courier
 * webhook events. {@code dispatchedAt} and {@code deliveredAt} are set once and
 * must not be overwritten. Optimistic locking prevents concurrent status updates.
 */
@Entity
@Table(
    schema = "shipping",
    name = "shipments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_shipments_order", columnNames = "order_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Shipment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shipment_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID shipmentId;

    /** The order this shipment fulfils. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_shipments_order"))
    private Order order;

    /** Carrier company name (e.g. "Blue Dart", "DTDC"). */
    @Column(name = "carrier", length = 100)
    private String carrier;

    /** Courier's own tracking number. */
    @Column(name = "tracking_number", length = 200)
    private String trackingNumber;

    /** Deep-link URL to the carrier's tracking portal. */
    @Column(name = "tracking_url", length = 2000)
    private String trackingUrl;

    /** Carrier's estimated delivery date. */
    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    /** Current shipment status. DB CHECK enforces the state machine. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    /** Timestamp when the shipment was physically dispatched. Set once. */
    @Column(name = "dispatched_at")
    private OffsetDateTime dispatchedAt;

    /** Timestamp when the shipment was delivered. Set once. */
    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    /** Tracking events for this shipment. */
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<ShipmentEvent> events = new ArrayList<>();

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Shipment lifecycle states. */
    public enum ShipmentStatus {
        CREATED,
        READY_FOR_DISPATCH,
        DISPATCHED,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }
}
