package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Summary of reviews and rating distribution.
 * <p>
 * Why: Bundles average rating, total review count, and star distribution for book display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDTO {
    private BigDecimal averageRating;
    private Integer totalReviews;
    private RatingDistributionDTO distribution;
}
