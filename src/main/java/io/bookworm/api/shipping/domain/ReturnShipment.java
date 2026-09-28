package io.bookworm.api.shipping.domain;

import io.bookworm.api.checkout.domain.ReturnRequest;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks the reverse logistics shipment for an approved return request.
 * <p>
 * Why: return shipments have a different state machine and carrier flow from
 * outbound shipments. One return shipment per return request is enforced by
 * a partial unique index. The {@code pickedUpAt} and {@code receivedAt} timestamps
 * drive the return request's status transitions to IN_TRANSIT and RECEIVED.
 * <p>
 * Side effects: receiving the return shipment (setting {@code receivedAt}) should
 * trigger a refund initiation in the payment bounded context via a domain event.
 */
@Entity
@Table(
    schema = "shipping",
    name = "return_shipments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_return_shipments_return_request",
                columnNames = "return_request_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReturnShipment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "return_shipment_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID returnShipmentId;

    /** The return request this shipment handles. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_return_shipments_return_request"))
    private ReturnRequest returnRequest;

    /** Carrier handling the reverse logistics. */
    @Column(name = "carrier", length = 100)
    private String carrier;

    /** Courier's tracking number for the return parcel. */
    @Column(name = "tracking_number", length = 200)
    private String trackingNumber;

    /** Tracking URL for the return parcel. */
    @Column(name = "tracking_url", length = 2000)
    private String trackingUrl;

    /** Current return shipment status. DB CHECK enforces allowed values. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReturnShipmentStatus status = ReturnShipmentStatus.CREATED;

    /** Timestamp when the courier picked up the parcel from the customer. */
    @Column(name = "picked_up_at")
    private OffsetDateTime pickedUpAt;

    /** Timestamp when the warehouse confirmed receipt of the return. */
    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Return shipment lifecycle states. */
    public enum ReturnShipmentStatus {
        CREATED,
        AWAITING_PICKUP,
        PICKED_UP,
        IN_TRANSIT,
        RECEIVED
    }
}
