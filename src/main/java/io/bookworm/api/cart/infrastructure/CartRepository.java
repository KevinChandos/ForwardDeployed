package io.bookworm.api.cart.infrastructure;

import io.bookworm.api.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Cart} entities.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /** Finds the active cart for an authenticated member. */
    @Query("""
           SELECT c FROM Cart c
           WHERE c.member.memberId = :memberId
             AND c.status = 'ACTIVE'
             AND c.deletedAt IS NULL
           """)
    Optional<Cart> findActiveMemberCart(@Param("memberId") UUID memberId);

    /**
     * Finds the active cart for a guest identified by their stable session token.
     */
    @Query("""
           SELECT c FROM Cart c
           WHERE c.guestToken = :guestToken
             AND c.status = 'ACTIVE'
             AND c.deletedAt IS NULL
           """)
    Optional<Cart> findActiveGuestCart(@Param("guestToken") String guestToken);
}
