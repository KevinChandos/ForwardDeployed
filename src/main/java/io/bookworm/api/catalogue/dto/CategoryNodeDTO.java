package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Hierarchical category node DTO for recursive tree navigation.
 * <p>
 * Why: Renders nested categories (e.g., Fiction -> Sci-Fi -> Cyberpunk) with child count and subtree elements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryNodeDTO {
    private UUID categoryId;
    private String name;
    private String slug;
    private Integer bookCount;
    private List<CategoryNodeDTO> children;
}
