package io.bookworm.api.auth.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A saved delivery/billing address belonging to a Member.
 * <p>
 * Why: members need to store multiple addresses (home, office, etc.) for reuse
 * during checkout. The {@code isDefault} flag drives auto-selection in the checkout
 * flow; only one active address per member should carry this flag, enforced at the
 * application layer.
 * <p>
 * Side effects: soft-delete ({@code deletedAt != null}) is used for removal so
 * historical orders that reference a snapshot of the address remain traceable.
 */
@Entity
@Table(schema = "identity", name = "member_addresses")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MemberAddress extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "address_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID addressId;

    /**
     * Owning member. Lazy because addresses are rarely needed alongside the Member.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_member_addresses_member"))
    private Member member;

    /** Optional human-readable label e.g. "Home", "Office". */
    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "line1", nullable = false, length = 250)
    private String line1;

    @Column(name = "line2", length = 250)
    private String line2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "pin_code", nullable = false, length = 20)
    private String pinCode;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    /**
     * ISO-3166 alpha-2 country code (e.g. "IN").
     * Stored as CHAR(2); defaults to India per the API spec.
     */
    @Column(name = "country", nullable = false, columnDefinition = "CHAR(2)")
    private String country = "IN";

    /**
     * Whether this is the member's default address for checkout.
     * Application layer must ensure at most one active address per member is default.
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;
}
