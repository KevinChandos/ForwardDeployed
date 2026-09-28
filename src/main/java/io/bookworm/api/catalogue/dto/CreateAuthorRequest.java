package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new author.
 * <p>
 * Why: Validates author name, bio, and external photo URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuthorRequest {

    @NotBlank(message = "Author name is required")
    @Size(max = 300, message = "Author name cannot exceed 300 characters")
    private String name;

    private String bio;

    @Size(max = 2000, message = "Photo URL cannot exceed 2000 characters")
    private String photoUrl;
}
