package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating a publisher.
 * <p>
 * Why: Allows partial modification of publisher details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePublisherRequest {

    @Size(min = 1, max = 300, message = "Publisher name must be between 1 and 300 characters")
    private String name;

    @Size(max = 2000, message = "Website URL cannot exceed 2000 characters")
    private String website;
}
