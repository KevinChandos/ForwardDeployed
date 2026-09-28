package io.bookworm.api.user.dto;

import io.bookworm.api.catalogue.dto.BookSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Item entry within a member's wishlist.
 * <p>
 * Why: Associates target book and format edition metadata with the item's creation timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDTO {
    private UUID wishlistItemId;
    private BookSummaryDTO book;
    private UUID bookFormatId;
    private String formatType;
    private OffsetDateTime addedAt;
}
