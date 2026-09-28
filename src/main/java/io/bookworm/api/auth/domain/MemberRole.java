package io.bookworm.api.auth.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single role grant for a Member.
 * <p>
 * Why: roles are stored as separate rows (not a VARCHAR array) so that
 * grant/revoke operations are simple INSERT/soft-DELETE statements, and role
 * history is preserved. The composite unique constraint prevents duplicate
 * active role assignments.
 * <p>
 * Side effects: soft-deleting this row revokes the role. The application must
 * re-check role grants on each authenticated request rather than caching them
 * inside the JWT, unless the JWT TTL is short enough to tolerate stale roles.
 */
@Entity
@Table(
    schema = "identity",
    name = "member_roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_member_roles_member_role",
                columnNames = {"member_id", "role_name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MemberRole extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_role_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID memberRoleId;

    /** Owning member. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_member_roles_member"))
    private Member member;

    /**
     * Role name stored as VARCHAR for schema portability.
     * Application layer maps to the {@link RoleName} enum before using it
     * with Spring Security's role-checking infrastructure.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 50)
    private RoleName roleName;

    /** Timestamp when the role was first granted to the member. */
    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt = OffsetDateTime.now();

    // ── Enum ────────────────────────────────────────────────────────────────

    /**
     * All role values defined in the OpenAPI MemberSummaryDTO schema.
     * Must stay in sync with the CHECK constraint in identity.member_roles.
     */
    public enum RoleName {
        GUEST,
        REGISTERED_USER,
        STORE_ADMIN,
        CATALOGUE_MANAGER,
        PLATFORM_ADMIN
    }
}
