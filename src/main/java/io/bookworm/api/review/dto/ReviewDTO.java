package io.bookworm.api.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public review representation for a book.
 * <p>
 * Why: Transports review text, star rating, author name, status, and whether it belongs to current authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private UUID reviewId;
    private String memberName;
    private Integer rating;
    private String body;
    private String status;
    private OffsetDateTime publishedAt;
    private OffsetDateTime submittedAt;
    private Boolean isOwn;
}
