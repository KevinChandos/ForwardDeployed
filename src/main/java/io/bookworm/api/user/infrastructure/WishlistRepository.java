package io.bookworm.api.user.infrastructure;

import io.bookworm.api.user.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Wishlist} entities.
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    /** Finds the active wishlist for a given member. */
    @Query("""
           SELECT w FROM Wishlist w
           WHERE w.member.memberId = :memberId
             AND w.deletedAt IS NULL
           """)
    Optional<Wishlist> findActiveByMemberId(@Param("memberId") UUID memberId);
}
