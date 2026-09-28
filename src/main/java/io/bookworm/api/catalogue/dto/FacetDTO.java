package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single facet bucket entry with filter value and matching item count.
 * <p>
 * Why: Renders dynamic search facet counts for search filters (categories, languages, authors, formats).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetDTO {
    private String value;
    private Long count;
}
