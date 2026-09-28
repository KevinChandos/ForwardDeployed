package io.bookworm.api.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard pagination metadata DTO.
 * <p>
 * Why: Encapsulates page index, page size, total record count, and total page count for all paginated endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDTO {

    @NotNull(message = "Page is required")
    @Min(value = 1, message = "Page number must be at least 1")
    private Integer page;

    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Page size must be at least 1")
    private Integer size;

    @NotNull(message = "Total is required")
    @Min(value = 0, message = "Total items count must be non-negative")
    private Long total;

    @NotNull(message = "TotalPages is required")
    @Min(value = 0, message = "Total pages count must be non-negative")
    private Integer totalPages;
}
