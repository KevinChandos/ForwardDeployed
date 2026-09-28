package io.bookworm.api.cart.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shopping cart — supports both guest and authenticated member carts.
 * <p>
 * Why: a single Cart entity handles both guest (identified by {@code guestToken})
 * and member carts. The merge operation (POST /cart/merge) transitions items from
 * a guest cart into the member's cart and sets the guest cart status to MERGED.
 * Only one ACTIVE cart per member/guest is permitted (partial unique indexes).
 * <p>
 * Side effects: status transitions are: ACTIVE → CHECKING_OUT (checkout initiated)
 * → CONVERTED (order confirmed) or back to ACTIVE (checkout abandoned/expired).
 * MERGED is terminal — a guest cart that was absorbed into a member cart.
 */
@Entity
@Table(
    schema = "cart",
    name = "carts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_carts_member_active", columnNames = "member_id"),
        @UniqueConstraint(name = "uq_carts_guest_active", columnNames = "guest_token")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cart extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cart_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID cartId;

    /**
     * Owning member. NULL for guest carts.
     * Either memberId or guestToken must be non-null (DB CHECK).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",
                foreignKey = @ForeignKey(name = "fk_carts_member"))
    private Member member;

    /**
     * Stable guest session identifier from X-Guest-Token header.
     * NULL for authenticated member carts.
     */
    @Column(name = "guest_token", length = 200)
    private String guestToken;

    /** Cart lifecycle status. DB CHECK enforces allowed values. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status = CartStatus.ACTIVE;

    /** Line items in this cart. */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Allowed cart lifecycle states. */
    public enum CartStatus {
        ACTIVE,
        CHECKING_OUT,
        CONVERTED,
        MERGED
    }
}
