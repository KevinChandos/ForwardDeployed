package io.bookworm.api.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for adding an item to the wishlist.
 * <p>
 * Why: Captures both book and specific format edition requested by the member.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddWishlistItemRequest {

    @NotNull(message = "Book ID is required")
    private UUID bookId;

    @NotNull(message = "Book format ID is required")
    private UUID bookFormatId;
}
