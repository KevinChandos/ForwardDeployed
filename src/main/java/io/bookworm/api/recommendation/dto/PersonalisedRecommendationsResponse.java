package io.bookworm.api.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Personalised recommendation list response DTO.
 * <p>
 * Why: Transports ranked recommendation items tailored to member's reading preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalisedRecommendationsResponse {
    private List<RecommendationItemDTO> data;
}
