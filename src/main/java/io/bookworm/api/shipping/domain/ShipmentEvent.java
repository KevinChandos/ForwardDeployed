package io.bookworm.api.shipping.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single tracking event in a Shipment's timeline (e.g. "PICKED_UP in Mumbai").
 * <p>
 * Why: tracking events form an append-only timeline that is pushed by the courier
 * webhook or manually entered by store staff. Each event is immutable after
 * insertion. The {@code occurredAt} timestamp comes from the courier, not from
 * the insert time ({@code createdAt}), to correctly represent the physical timeline.
 * <p>
 * Side effects: events are ordered by {@code occurredAt} on the Shipment entity.
 * Soft-delete is included for schema consistency but should not be used to hide
 * legitimate tracking events from customers.
 */
@Entity
@Table(schema = "shipping", name = "shipment_events")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShipmentEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID eventId;

    /** The shipment this event belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_shipment_events_shipment"))
    private Shipment shipment;

    /**
     * Descriptive event type (e.g. "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY").
     * Free-form string — not an enum — because carrier event types vary by provider.
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Location description at the time of the event (e.g. "Mumbai Hub"). */
    @Column(name = "location", length = 300)
    private String location;

    /** Optional additional notes from the courier. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * When the physical event occurred according to the courier.
     * This differs from {@code createdAt} when the webhook arrives late.
     */
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}
