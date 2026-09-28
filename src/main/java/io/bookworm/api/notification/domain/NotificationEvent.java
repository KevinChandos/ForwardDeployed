package io.bookworm.api.notification.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A queued or sent notification event for a specific recipient.
 * <p>
 * Why: decoupling notification dispatch from business events via this table allows
 * a dedicated notification worker to process the queue independently. PENDING rows
 * are picked up and sent; FAILED rows can be retried. The {@code payload} JSONB
 * column holds the template variable data specific to this event instance.
 * <p>
 * Side effects: {@code sentAt} is set once by the worker when the external
 * provider confirms delivery. If the provider returns an error, {@code status}
 * becomes FAILED and {@code failureReason} records the error. BOUNCED status
 * indicates the message was accepted by the provider but not delivered
 * (e.g. invalid email address).
 */
@Entity
@Table(schema = "notification", name = "notification_events")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID notificationId;

    /**
     * The target member. NULL for guest notifications where only an email
     * address is available.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_member_id",
                foreignKey = @ForeignKey(name = "fk_notification_events_member"))
    private Member recipientMember;

    /**
     * Fallback recipient email for guest notifications.
     * NULL when {@code recipientMember} is set.
     */
    @Column(name = "recipient_email", length = 320)
    private String recipientEmail;

    /**
     * Delivery channel. DB CHECK enforces EMAIL | SMS | PUSH.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationTemplate.NotificationChannel channel;

    /**
     * Template code reference. FK to notification_templates.
     * Not modelled as a JPA @ManyToOne to avoid eager loading the template body
     * every time an event is fetched.
     */
    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    /**
     * JSONB payload holding the Handlebars/Mustache variable values for this event.
     * Stored as a {@code Map<String, Object>} and mapped via Hibernate's JSON type.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /** Delivery status. DB CHECK enforces allowed values. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    /** Timestamp when the provider confirmed delivery. */
    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    /** Provider error description when status is FAILED or BOUNCED. */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Notification delivery lifecycle states. */
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        BOUNCED
    }
}
