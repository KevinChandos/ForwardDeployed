package io.bookworm.api.review.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated review list response DTO.
 * <p>
 * Why: Wraps list of reviews with pagination metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListResponse {
    private List<ReviewDTO> data;
    private PaginationDTO pagination;
}
