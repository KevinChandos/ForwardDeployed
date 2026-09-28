package io.bookworm.api.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating an existing review.
 * <p>
 * Why: Accepts revised star rating, review text, and entity version for optimistic locking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(min = 10, max = 2000, message = "Review body must be between 10 and 2000 characters")
    private String body;

    @NotNull(message = "Version is required for optimistic locking")
    @Min(value = 1, message = "Version must be at least 1")
    private Integer version;
}
