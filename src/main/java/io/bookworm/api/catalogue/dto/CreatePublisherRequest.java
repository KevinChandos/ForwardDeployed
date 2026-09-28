package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new publisher.
 * <p>
 * Why: Validates publisher name and website address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePublisherRequest {

    @NotBlank(message = "Publisher name is required")
    @Size(max = 300, message = "Publisher name cannot exceed 300 characters")
    private String name;

    @Size(max = 2000, message = "Website URL cannot exceed 2000 characters")
    private String website;
}
