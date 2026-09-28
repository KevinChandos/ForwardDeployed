package io.bookworm.api.recommendation.mapper;

import io.bookworm.api.catalogue.mapper.CatalogueMapper;
import io.bookworm.api.recommendation.domain.RecommendedBook;
import io.bookworm.api.recommendation.dto.RecommendationItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Recommendations bounded context.
 * <p>
 * Why: Transforms RecommendedBook entity records into RecommendationItemDTOs.
 */
@Mapper(
    componentModel = "spring",
    uses = {CatalogueMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RecommendationMapper {

    @Mapping(target = "book", source = "recommendedBook.book")
    @Mapping(target = "score", source = "recommendedBook.score")
    @Mapping(target = "reason", source = "recommendedBook.reason")
    RecommendationItemDTO toRecommendationItemDTO(RecommendedBook recommendedBook);

    List<RecommendationItemDTO> toRecommendationItemDTOList(List<RecommendedBook> recommendedBooks);
}
