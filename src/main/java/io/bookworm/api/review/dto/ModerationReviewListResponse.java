package io.bookworm.api.review.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated moderation review list response.
 * <p>
 * Why: Transports pending moderation reviews alongside pagination metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationReviewListResponse {
    private List<ModerationReviewDTO> data;
    private PaginationDTO pagination;
}
