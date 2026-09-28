package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for updating a category.
 * <p>
 * Why: Allows partial modification of category name, slug, and hierarchical parent category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    @Size(min = 1, max = 150, message = "Category name must be between 1 and 150 characters")
    private String name;

    @Size(min = 1, max = 150, message = "Slug must be between 1 and 150 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    private String slug;

    private UUID parentCategoryId;
}
