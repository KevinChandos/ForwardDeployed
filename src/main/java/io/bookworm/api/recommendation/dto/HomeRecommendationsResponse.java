package io.bookworm.api.recommendation.dto;

import io.bookworm.api.catalogue.dto.BookSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Storefront home recommendations response DTO.
 * <p>
 * Why: Bundles curated book carousels: personal recommendations, bestsellers of the month, and new launches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeRecommendationsResponse {
    private List<BookSummaryDTO> recommendedForYou;
    private List<BookSummaryDTO> bestsellersThisMonth;
    private List<BookSummaryDTO> newLaunches;
}
