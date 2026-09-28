package io.bookworm.api.cart.application;

import io.bookworm.api.cart.dto.*;

import java.util.UUID;

/**
 * Service interface for shopping cart operations.
 * <p>
 * Why: Supports dual guest/member cart resolution, item additions/updates/removals,
 * pricing recalculation, and guest-to-member cart merging.
 */
public interface CartService {

    /**
     * Retrieves the active cart for either an authenticated member or a guest session.
     */
    CartResponse getCart(UUID memberId, String guestToken, UUID storeId);

    /**
     * Adds an item to the user's cart (or increments quantity if already present).
     */
    CartResponse addItem(UUID memberId, String guestToken, UUID storeId, AddCartItemRequest request);

    /**
     * Updates the quantity of a specific item in the cart.
     */
    CartResponse updateItemQuantity(UUID memberId, String guestToken, UUID storeId, UUID itemId, UpdateCartItemRequest request);

    /**
     * Removes an item from the cart.
     */
    void removeItem(UUID memberId, String guestToken, UUID itemId);

    /**
     * Merges a guest cart into the authenticated member's cart on login.
     */
    CartResponse mergeCart(UUID memberId, UUID storeId, MergeCartRequest request);
}
