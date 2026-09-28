package io.bookworm.api.user.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A member's saved wishlist — a named collection of desired book formats.
 * <p>
 * Why: the wishlist is distinct from the cart; items are saved without intent to
 * purchase immediately. Each member has at most one active wishlist (partial unique
 * index enforces this). Soft-delete of the wishlist cascades conceptually but not
 * via Hibernate cascade — items are soft-deleted independently.
 * <p>
 * Side effects: guest users do not have wishlists (member_id is NOT NULL).
 */
@Entity
@Table(
    schema = "cart",
    name = "wishlists",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_wishlists_member", columnNames = "member_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Wishlist extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wishlist_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID wishlistId;

    /** The member who owns this wishlist. One per member (unique constraint). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wishlists_member"))
    private Member member;

    /** Items saved in this wishlist. */
    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WishlistItem> items = new ArrayList<>();
}
