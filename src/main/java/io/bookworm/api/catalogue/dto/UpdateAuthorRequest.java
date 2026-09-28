package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating an author.
 * <p>
 * Why: Allows partial modification of author details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAuthorRequest {

    @Size(min = 1, max = 300, message = "Author name must be between 1 and 300 characters")
    private String name;

    private String bio;

    @Size(max = 2000, message = "Photo URL cannot exceed 2000 characters")
    private String photoUrl;
}
