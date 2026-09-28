package io.bookworm.api.auth.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stores a hashed password credential for a Member on a specific channel.
 * <p>
 * Why: credentials are separated from the Member aggregate root so that
 * the password hash is never inadvertently included in Member query projections.
 * A single member may hold both EMAIL and PHONE credentials simultaneously.
 * <p>
 * Side effects: {@code hashedSecret} must always be stored as a bcrypt or
 * Argon2 hash — never plaintext. The {@code resetToken} is single-use and must
 * be nulled out after a successful password reset.
 */
@Entity
@Table(
    schema = "identity",
    name = "credentials",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_credentials_member_channel",
                columnNames = {"member_id", "channel"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Credential extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credential_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID credentialId;

    /**
     * Owning member. FK to identity.members. Eagerly loaded because credentials
     * are always fetched with the member during authentication.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_credentials_member"))
    private Member member;

    /**
     * Authentication channel. CHECK constraint enforces EMAIL | PHONE at DB level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private Channel channel;

    /**
     * bcrypt/Argon2 password hash. Never exposed in API responses.
     */
    @Column(name = "hashed_secret", nullable = false, length = 255)
    private String hashedSecret;

    /**
     * Single-use reset token (hashed). NULL when no reset is in flight.
     * Nulled out after successful consumption.
     */
    @Column(name = "reset_token", length = 255)
    private String resetToken;

    /**
     * Expiry timestamp for the reset token. Must be checked before allowing reset.
     */
    @Column(name = "reset_expires_at")
    private OffsetDateTime resetExpiresAt;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Credential delivery channel. */
    public enum Channel {
        EMAIL,
        PHONE
    }
}
