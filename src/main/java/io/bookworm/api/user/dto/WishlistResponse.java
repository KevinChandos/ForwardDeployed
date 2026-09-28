package io.bookworm.api.user.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated wishlist response DTO.
 * <p>
 * Why: Wraps list of wishlist item DTOs with pagination metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private List<WishlistItemDTO> data;
    private PaginationDTO pagination;
}
