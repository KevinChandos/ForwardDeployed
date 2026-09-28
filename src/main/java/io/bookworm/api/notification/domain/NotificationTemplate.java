package io.bookworm.api.notification.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Reusable template for system-generated notifications (email, SMS, push).
 * <p>
 * Why: centralising message templates in the database enables non-developer
 * customisation of notification copy without a code deploy. Templates use
 * Handlebars/Mustache syntax; the {@code payload} JSON in each NotificationEvent
 * provides the variables. The primary key is the natural {@code templateCode} string
 * rather than a UUID, to make template references self-documenting in code.
 * <p>
 * Side effects: {@code isActive = false} prevents new events from being sent
 * using this template. In-flight events that were already queued are unaffected.
 */
@Entity
@Table(schema = "notification", name = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationTemplate extends AuditableEntity {

    /**
     * Natural primary key — a descriptive code such as {@code ORDER_CONFIRMED_EMAIL}.
     * Used as a foreign key in NotificationEvent.
     */
    @Id
    @Column(name = "template_code", updatable = false, nullable = false, length = 100)
    @ToString.Include
    @EqualsAndHashCode.Include
    private String templateCode;

    /**
     * Email subject line. Only relevant for EMAIL channel; null for SMS/PUSH.
     */
    @Column(name = "subject", length = 500)
    private String subject;

    /**
     * Handlebars/Mustache template body. Variables are provided via the
     * NotificationEvent's {@code payload} JSON field.
     */
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    /**
     * The delivery channel this template targets.
     * DB CHECK enforces EMAIL | SMS | PUSH.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    /** Whether this template is available for use. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Supported notification delivery channels. */
    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH
    }
}
