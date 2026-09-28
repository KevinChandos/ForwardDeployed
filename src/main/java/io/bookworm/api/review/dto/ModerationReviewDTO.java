package io.bookworm.api.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Moderation queue review representation.
 * <p>
 * Why: Transports pending reviews with associated book and reviewer details for moderation workflows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationReviewDTO {
    private UUID reviewId;
    private UUID bookId;
    private String bookTitle;
    private UUID memberId;
    private String memberName;
    private Integer rating;
    private String body;
    private OffsetDateTime submittedAt;
}
