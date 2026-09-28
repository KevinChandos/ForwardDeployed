package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Category tree response DTO.
 * <p>
 * Why: Encapsulates list of root categories each having their recursive child nodes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeResponse {
    private List<CategoryNodeDTO> data;
}
