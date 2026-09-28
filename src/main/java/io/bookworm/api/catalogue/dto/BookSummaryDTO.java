package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Summary representation of a book for catalogue listings, searches, and cart/wishlist items.
 * <p>
 * Why: Carries essential book details including starting price and rating aggregates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSummaryDTO {
    private UUID bookId;
    private String title;
    private String coverImageUrl;
    private List<AuthorRefDTO> authors;
    private List<FormatPriceDTO> formats;
    private MoneyDTO startingPrice;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private Boolean isActive;
}
