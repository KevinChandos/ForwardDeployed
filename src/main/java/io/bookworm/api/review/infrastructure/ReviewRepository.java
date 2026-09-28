package io.bookworm.api.review.infrastructure;

import io.bookworm.api.review.domain.Review;
import io.bookworm.api.review.domain.Review.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Review} entities.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Returns the published reviews for a book, most recent first.
     * Used for the public book reviews endpoint.
     */
    @Query(value = """
           SELECT r FROM Review r
           WHERE r.book.bookId = :bookId
             AND r.status = 'PUBLISHED'
             AND r.deletedAt IS NULL
           ORDER BY r.publishedAt DESC
           """,
           countQuery = """
           SELECT COUNT(r) FROM Review r
           WHERE r.book.bookId = :bookId
             AND r.status = 'PUBLISHED'
             AND r.deletedAt IS NULL
           """)
    Page<Review> findPublishedByBookId(
            @Param("bookId") UUID bookId,
            Pageable pageable);

    /**
     * Finds the current member's review for a book (any status).
     * Used for the GET /books/{bookId}/reviews/me endpoint.
     */
    @Query("""
           SELECT r FROM Review r
           WHERE r.book.bookId = :bookId
             AND r.member.memberId = :memberId
             AND r.deletedAt IS NULL
           """)
    Optional<Review> findByBookAndMember(
            @Param("bookId") UUID bookId,
            @Param("memberId") UUID memberId);

    /**
     * Pages through reviews pending moderation — used by the moderation queue.
     */
    @Query(value = """
           SELECT r FROM Review r
           WHERE r.status = 'PENDING'
             AND r.deletedAt IS NULL
           ORDER BY r.submittedAt ASC
           """,
           countQuery = """
           SELECT COUNT(r) FROM Review r
           WHERE r.status = 'PENDING'
             AND r.deletedAt IS NULL
           """)
    Page<Review> findPendingModeration(Pageable pageable);
}
