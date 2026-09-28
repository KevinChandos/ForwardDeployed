package io.bookworm.api.outbox.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Transactional outbox pattern — stores domain events atomically with aggregate
 * mutations so they are guaranteed to be published to the event bus eventually.
 * <p>
 * Why: writing a domain event to this table in the same DB transaction as the
 * aggregate mutation ensures that the event is never lost if the process crashes
 * between the DB commit and the Kafka/SQS publish. A separate relay process
 * polls PENDING rows and publishes them, then marks them PUBLISHED. This avoids
 * distributed transaction requirements (2PC) entirely.
 * <p>
 * Side effects: this entity intentionally does NOT extend {@link io.bookworm.api.common.domain.AuditableEntity}
 * because (a) it has no soft-delete semantics, (b) createdBy/updatedBy are not
 * meaningful for machine-generated outbox rows, and (c) the version column would
 * conflict with relay-side idempotency logic. Audit timestamps are included
 * directly without the full superclass.
 */
@Entity
@Table(
    schema = "outbox",
    name = "domain_event_outbox",
    indexes = {
        @Index(name = "idx_outbox_status_occurred",
               columnList = "status, occurred_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DomainEventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID eventId;

    /**
     * Fully-qualified event type name (e.g. "OrderPlaced", "PaymentConfirmed").
     * Used by consumers to determine the schema of {@code payload}.
     */
    @Column(name = "event_type", nullable = false, length = 200)
    @ToString.Include
    private String eventType;

    /**
     * The domain aggregate type that produced the event (e.g. "Order", "PaymentTransaction").
     * Used for routing and filtering by consumers.
     */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /** UUID of the specific aggregate instance that produced the event. */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /**
     * Full event payload as JSONB. The schema is determined by {@code eventType}.
     * Consumers must deserialise based on the event type.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /**
     * Relay status. PENDING rows are picked up by the relay process.
     * PUBLISHED rows have been successfully sent to the event bus.
     * FAILED rows exceeded retry attempts and require manual intervention.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    /** When the domain event logically occurred (set by the aggregate layer). */
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    /**
     * Timestamp when the relay process confirmed publication to the event bus.
     * NULL until PUBLISHED.
     */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /**
     * Number of relay attempts made. Relay process increments this on each
     * try before marking FAILED after a configured threshold.
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /** Row creation timestamp set by Spring Data auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Last modification timestamp set by Spring Data auditing. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Outbox event relay states. */
    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
