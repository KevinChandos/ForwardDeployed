package io.bookworm.api.payment.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records a single attempt to settle a PaymentTransaction through the gateway.
 * <p>
 * Why: when a payment fails and is retried, each attempt needs to be logged for
 * reconciliation and fraud analysis. Keeping attempts as a separate entity leaves
 * the parent transaction clean and avoids array columns or JSON blobs. This table
 * is append-only — rows are never updated after insert.
 * <p>
 * Side effects: {@code gatewayStatus} holds the raw string returned by the gateway
 * (e.g. "failed", "captured", "authorization_failed") for direct auditability.
 */
@Entity
@Table(schema = "payment", name = "payment_attempts")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentAttempt extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attempt_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID attemptId;

    /** The parent transaction this attempt belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_payment_attempts_transaction"))
    private PaymentTransaction transaction;

    /** Timestamp when this attempt was made. */
    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt = OffsetDateTime.now();

    /**
     * Raw status code/message returned by the gateway for this attempt.
     * Stored verbatim for audit; do not use for business logic — use the
     * parent transaction's {@link PaymentTransaction#getStatus()} instead.
     */
    @Column(name = "gateway_status", nullable = false, length = 100)
    private String gatewayStatus;

    /**
     * Human-readable failure reason when the attempt failed.
     * NULL for successful attempts.
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
