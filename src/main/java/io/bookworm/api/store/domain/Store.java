package io.bookworm.api.store.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a store (sales channel) in the Book Worm platform.
 * <p>
 * Why: the multi-store model allows different pricing, tax rules, and shipping
 * thresholds per regional or brand-specific store. All catalogue prices, coupons,
 * and orders are scoped to a store. The {@code slug} is used in API routing and
 * must be unique to prevent URL collisions.
 * <p>
 * Side effects: INACTIVE stores cannot accept new orders. Existing orders are
 * unaffected by status changes — a store going INACTIVE is not a cascade to orders.
 */
@Entity
@Table(
    schema = "store",
    name = "stores",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_stores_slug", columnNames = "slug")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Store extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID storeId;

    @Column(name = "name", nullable = false, length = 200)
    @ToString.Include
    private String name;

    /**
     * URL-safe store identifier. Unique across active stores.
     * Used as a path segment in storefront routing.
     */
    @Column(name = "slug", nullable = false, length = 200)
    private String slug;

    /** Optional geographic region descriptor (e.g. "IN-SOUTH"). */
    @Column(name = "region", length = 100)
    private String region;

    /** Lifecycle status. INACTIVE stores block new orders. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreStatus status = StoreStatus.ACTIVE;

    /**
     * The member who owns / manages this store. May be null for
     * system-level stores created during bootstrap.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_member_id",
                foreignKey = @ForeignKey(name = "fk_stores_owner_member"))
    private Member ownerMember;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Allowed store lifecycle states. */
    public enum StoreStatus {
        ACTIVE,
        INACTIVE
    }
}
