package io.bookworm.api.review.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A member's star-rating and written review for a Book.
 * <p>
 * Why: reviews are moderated before being publicly visible — they enter the
 * PENDING state on submission and must be explicitly PUBLISHED by a moderator.
 * A rejected review carries a {@code rejectionReason}. One active review per
 * (book, member) pair is enforced by a partial unique index.
 * <p>
 * Side effects: publishing a review triggers a domain event that updates the
 * {@code averageRating} and {@code reviewCount} denormalised columns on Book.
 * Soft-deleting a published review should also trigger an event to recompute those
 * counters. Optimistic locking prevents concurrent moderation conflicts.
 */
@Entity
@Table(
    schema = "review",
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_reviews_book_member",
                columnNames = {"book_id", "member_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Review extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID reviewId;

    /** The reviewed book. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_reviews_book"))
    private Book book;

    /** The reviewing member. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_reviews_member"))
    private Member member;

    /**
     * Star rating (1–5). Stored as SMALLINT.
     * DB CHECK: rating BETWEEN 1 AND 5.
     */
    @Column(name = "rating", nullable = false, columnDefinition = "SMALLINT")
    private Short rating;

    /**
     * Optional written review body (10–2000 chars per API spec).
     * NULL means a rating-only review.
     */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** Moderation status. DB CHECK enforces allowed values. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    /** Timestamp when the member submitted the review. */
    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt = OffsetDateTime.now();

    /**
     * Timestamp when the review was published (made publicly visible).
     * NULL until the review is PUBLISHED.
     */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /**
     * Reason supplied by the moderator when rejecting the review.
     * NULL for non-rejected reviews.
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Review moderation states. */
    public enum ReviewStatus {
        PENDING,
        PUBLISHED,
        REJECTED
    }
}
