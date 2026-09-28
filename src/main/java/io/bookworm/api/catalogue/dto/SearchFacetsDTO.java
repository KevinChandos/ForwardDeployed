package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated search facets including categories, languages, formats, authors, and price ranges.
 * <p>
 * Why: Bundles all search filters returned alongside search query results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchFacetsDTO {
    private List<FacetDTO> categories;
    private List<FacetDTO> languages;
    private List<FacetDTO> formats;
    private List<FacetDTO> authors;
    private PriceRangeFacetDTO priceRange;
}
