package io.bookworm.api.promotions.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated coupon list response DTO.
 * <p>
 * Why: Transports list of coupons with pagination metadata for admin dashboards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponListResponse {
    private List<CouponDTO> data;
    private PaginationDTO pagination;
}
