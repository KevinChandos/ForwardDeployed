package io.bookworm.api.cart.infrastructure;

import io.bookworm.api.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link CartItem} entities.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /** Returns all active items in a cart. */
    @Query("""
           SELECT i FROM CartItem i
           WHERE i.cart.cartId = :cartId
             AND i.deletedAt IS NULL
           """)
    List<CartItem> findActiveByCartId(@Param("cartId") UUID cartId);

    /**
     * Finds a specific active item by its ID within a cart.
     * Used to validate ownership before quantity update or removal.
     */
    @Query("""
           SELECT i FROM CartItem i
           WHERE i.cartItemId = :itemId
             AND i.cart.cartId = :cartId
             AND i.deletedAt IS NULL
           """)
    Optional<CartItem> findActiveByIdAndCartId(
            @Param("itemId") UUID itemId,
            @Param("cartId") UUID cartId);

    /**
     * Finds an active item for a specific format in a cart.
     * Used to detect duplicates and increment quantity on re-add.
     */
    @Query("""
           SELECT i FROM CartItem i
           WHERE i.cart.cartId = :cartId
             AND i.bookFormat.bookFormatId = :formatId
             AND i.deletedAt IS NULL
           """)
    Optional<CartItem> findActiveByCartAndFormat(
            @Param("cartId") UUID cartId,
            @Param("formatId") UUID formatId);
}
