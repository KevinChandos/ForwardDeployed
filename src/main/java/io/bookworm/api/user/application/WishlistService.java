package io.bookworm.api.user.application;

import io.bookworm.api.user.dto.AddWishlistItemRequest;
import io.bookworm.api.user.dto.WishlistResponse;

import java.util.UUID;

/**
 * Service interface for managing member wishlists.
 * <p>
 * Why: Encapsulates business logic for wishlist lookups, adding book formats,
 * preventing duplicates, and removing items with ownership verification.
 */
public interface WishlistService {

    /**
     * Retrieves the active wishlist and its items for the given member.
     */
    WishlistResponse getWishlist(UUID memberId);

    /**
     * Adds a book format edition to the member's wishlist.
     */
    WishlistResponse addWishlistItem(UUID memberId, AddWishlistItemRequest request);

    /**
     * Removes an item from the member's wishlist.
     */
    void removeWishlistItem(UUID memberId, UUID itemId);
}
