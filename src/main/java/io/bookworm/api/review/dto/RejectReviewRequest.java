package io.bookworm.api.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for rejecting a review in the moderation queue.
 * <p>
 * Why: Captures the reason for rejection for audit and notification logging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectReviewRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}
