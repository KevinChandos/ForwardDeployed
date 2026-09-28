package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detailed book response representation.
 * <p>
 * Why: Encapsulates all book attributes, credited authors, publishers, categories, formats with prices, and review aggregates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailResponse {
    private UUID bookId;
    private String title;
    private String synopsis;
    private String language;
    private String coverImageUrl;
    private LocalDate publishedDate;
    private Integer salesCount;
    private Boolean isActive;
    private PublisherRefDTO publisher;
    private List<AuthorDetailRefDTO> authors;
    private List<CategoryRefDTO> categories;
    private List<FormatPriceDTO> formats;
    private MoneyDTO startingPrice;
    private ReviewSummaryDTO reviews;
    private DeliveryEstimateDTO deliveryEstimate;
}
