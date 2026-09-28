package io.bookworm.api.review.application;

import io.bookworm.api.review.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for customer book reviews and administrative moderation.
 * <p>
 * Why: Encapsulates review submission, rating bounds checking, verified buyer validation,
 * moderation queue traversal, approval/publishing, and rejection workflows.
 */
public interface ReviewService {

    /**
     * Pages all published reviews for a specific book.
     */
    ReviewListResponse getBookReviews(UUID bookId, UUID currentMemberId, Pageable pageable);

    /**
     * Retrieves the current member's review for a book (if present).
     */
    ReviewDTO getMyReview(UUID bookId, UUID memberId);

    /**
     * Submits a new review for moderation.
     */
    ReviewDTO submitReview(UUID bookId, UUID memberId, SubmitReviewRequest request);

    /**
     * Updates an existing review submitted by the member.
     */
    ReviewDTO updateReview(UUID bookId, UUID memberId, UpdateReviewRequest request);

    /**
     * Deletes a review.
     */
    void deleteReview(UUID bookId, UUID memberId);

    // ── Moderation Queue (Admin) ───────────────────────────────────────────────

    /**
     * Pages reviews pending moderation.
     */
    ModerationReviewListResponse getPendingReviews(Pageable pageable);

    /**
     * Publishes an approved review.
     */
    void approveReview(UUID reviewId);

    /**
     * Rejects a review with a reason.
     */
    void rejectReview(UUID reviewId, RejectReviewRequest request);
}
