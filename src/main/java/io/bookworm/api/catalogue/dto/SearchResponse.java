package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Catalogue and book search response representation.
 * <p>
 * Why: Transports paginated list of matching book summaries with facet breakdown for storefront filtering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private List<BookSummaryDTO> data;
    private SearchFacetsDTO facets;
    private PaginationDTO pagination;
}
