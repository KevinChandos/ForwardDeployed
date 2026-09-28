package io.bookworm.api.recommendation.dto;

import io.bookworm.api.catalogue.dto.BookSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Individual recommendation entry item.
 * <p>
 * Why: Associates recommended book summary with recommendation score and contextual reason.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemDTO {
    private BookSummaryDTO book;
    private BigDecimal score;
    private String reason;
}
