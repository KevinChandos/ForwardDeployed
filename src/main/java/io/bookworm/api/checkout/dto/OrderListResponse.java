package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated order list response DTO.
 * <p>
 * Why: Wraps list of order summaries with pagination metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {
    private List<OrderSummaryDTO> data;
    private PaginationDTO pagination;
}
