package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Breakdown of customer review ratings from 1 to 5 stars.
 * <p>
 * Why: Powers review histogram visualisations on book details pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDistributionDTO {
    private Integer oneStar;
    private Integer twoStar;
    private Integer threeStar;
    private Integer fourStar;
    private Integer fiveStar;
}
