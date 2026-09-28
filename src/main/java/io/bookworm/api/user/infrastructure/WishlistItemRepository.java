package io.bookworm.api.user.infrastructure;

import io.bookworm.api.user.domain.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link WishlistItem} entities.
 */
@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    /**
     * Finds an active wishlist item by its ID within a specific wishlist.
     * Used to validate ownership before removal.
     */
    @Query("""
           SELECT i FROM WishlistItem i
           WHERE i.wishlistItemId = :itemId
             AND i.wishlist.wishlistId = :wishlistId
             AND i.deletedAt IS NULL
           """)
    Optional<WishlistItem> findActiveByIdAndWishlistId(
            @Param("itemId") UUID itemId,
            @Param("wishlistId") UUID wishlistId);

    /**
     * Checks whether a specific book format is already on the wishlist.
     * Used to prevent duplicate wishlist entries.
     */
    @Query("""
           SELECT COUNT(i) > 0 FROM WishlistItem i
           WHERE i.wishlist.wishlistId = :wishlistId
             AND i.bookFormat.bookFormatId = :bookFormatId
             AND i.deletedAt IS NULL
           """)
    boolean existsActiveByWishlistAndFormat(
            @Param("wishlistId") UUID wishlistId,
            @Param("bookFormatId") UUID bookFormatId);
}
