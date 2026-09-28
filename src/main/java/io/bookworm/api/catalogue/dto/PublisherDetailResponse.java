package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Detailed publisher response representation.
 * <p>
 * Why: Transports publisher metadata along with published books and pagination information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherDetailResponse {
    private UUID publisherId;
    private String name;
    private String website;
    private List<BookSummaryDTO> books;
    private PaginationDTO pagination;
}
