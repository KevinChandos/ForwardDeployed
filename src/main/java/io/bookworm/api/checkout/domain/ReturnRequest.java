package io.bookworm.api.checkout.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a customer's request to return an order.
 * <p>
 * Why: return requests have their own state machine and lifecycle independent of
 * the Order. Keeping them as a separate entity allows multiple returns on a single
 * order (e.g. partial returns or rejected-then-re-requested returns) while keeping
 * the Order entity focused on the purchase lifecycle.
 * <p>
 * Side effects: {@code resolvedAt} is set when the request transitions to a terminal
 * state (REJECTED or REFUNDED). Soft-delete is not used as a workflow mechanism
 * here — state changes use status transitions.
 */
@Entity
@Table(schema = "ordering", name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReturnRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "return_request_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID returnRequestId;

    /** The order this return belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_return_requests_order"))
    private Order order;

    /** Current return status. DB CHECK enforces allowed values. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    /**
     * Customer-supplied reason for the return (5–200 chars per API spec).
     */
    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    /** Optional extended notes from the customer. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Timestamp when the customer submitted the return request. */
    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    /** Timestamp when the request was resolved (approved, rejected, or refunded). */
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Return request lifecycle states. */
    public enum ReturnStatus {
        REQUESTED,
        APPROVED,
        REJECTED,
        IN_TRANSIT,
        RECEIVED,
        REFUNDED
    }
}
