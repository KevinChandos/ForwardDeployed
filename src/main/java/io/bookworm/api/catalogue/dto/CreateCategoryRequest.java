package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for creating a category.
 * <p>
 * Why: Validates category name, URL-safe lowercase slug, and optional parent category for hierarchy positioning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 150, message = "Category name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 150, message = "Slug cannot exceed 150 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    private String slug;

    private UUID parentCategoryId;
}
