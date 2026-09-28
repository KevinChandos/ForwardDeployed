package io.bookworm.api.auth.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for the identity bounded context.
 * <p>
 * Why: the Member is the central identity entity that owns credentials, addresses,
 * sessions, roles, a wallet, and a wishlist. All relationships are represented as
 * FK constraints in the database; lazy loading is used for all collections to
 * avoid N+1 queries during authentication path (which only needs the Member itself).
 * <p>
 * Side effects: {@code status} drives Spring Security account locking. SUSPENDED
 * maps to {@code accountNonLocked = false}; CLOSED maps to {@code enabled = false}.
 */
@Entity
@Table(
    schema = "identity",
    name = "members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_members_email", columnNames = "email"),
        @UniqueConstraint(name = "uq_members_phone_number", columnNames = "phone_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Member extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID memberId;

    /** Display name shown to other users; mutable via profile update. */
    @Column(name = "display_name", nullable = false, length = 150)
    @ToString.Include
    private String displayName;

    /**
     * Normalised lowercase email. Either email or phoneNumber must be non-null.
     * Unique constraint is partial (WHERE email IS NOT NULL AND deleted_at IS NULL).
     */
    @Column(name = "email", length = 320)
    private String email;

    /**
     * E.164-format phone number (e.g. "+919876543210").
     * Either email or phoneNumber must be non-null.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Account status. CHECK constraint enforces allowed values at DB level.
     * Application-level validation uses the {@link MemberStatus} enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    /** Credentials associated with this member (EMAIL and/or PHONE channels). */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Credential> credentials = new ArrayList<>();

    /** Saved delivery addresses. */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MemberAddress> addresses = new ArrayList<>();

    /** Active sessions (each carries hashed tokens). */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Session> sessions = new ArrayList<>();

    /** Granted roles. */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MemberRole> roles = new ArrayList<>();

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Allowed lifecycle states for a Member account. */
    public enum MemberStatus {
        ACTIVE,
        SUSPENDED,
        CLOSED
    }
}
