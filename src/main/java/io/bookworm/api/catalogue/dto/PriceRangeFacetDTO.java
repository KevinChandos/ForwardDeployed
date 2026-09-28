package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Price range filter facet containing minimum and maximum price boundaries in current search results.
 * <p>
 * Why: Powers price slider widgets in catalogue search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRangeFacetDTO {
    private MoneyDTO min;
    private MoneyDTO max;
}
