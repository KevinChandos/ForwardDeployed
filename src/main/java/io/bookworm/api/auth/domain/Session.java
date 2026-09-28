package io.bookworm.api.auth.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an active authentication session for a Member.
 * <p>
 * Why: rather than storing JWTs themselves (which are stateless by design), we
 * store SHA-256 hashes of the access and refresh tokens. This enables server-side
 * revocation (logout) without compromising the tokens if the sessions table is read.
 * <p>
 * Side effects: soft-delete of this row is the logical logout action — the relay
 * process does NOT need to invalidate the JWT; the filter chain checks the session
 * table on each request. {@code expiresAt} is the wall-clock expiry; rows beyond
 * this timestamp are treated as invalid regardless of soft-delete state.
 */
@Entity
@Table(schema = "identity", name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Session extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID sessionId;

    /** Owning member. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sessions_member"))
    private Member member;

    /**
     * SHA-256 hash of the JWT access token. Never store the raw token.
     * Used to validate that the presented token was issued by this server
     * and has not been revoked.
     */
    @Column(name = "access_token_hash", nullable = false, length = 255)
    private String accessTokenHash;

    /**
     * SHA-256 hash of the refresh token. Single-use — nulled out after refresh.
     */
    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    /**
     * Hard expiry timestamp. Regardless of active status, sessions past this
     * time are rejected.
     */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * Optional user-agent / device fingerprint for display on the member's
     * "active sessions" screen.
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
}
