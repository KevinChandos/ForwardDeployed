package io.bookworm.api.recommendation.infrastructure;

import io.bookworm.api.recommendation.domain.RecommendedBook;
import io.bookworm.api.recommendation.domain.RecommendedBook.RecommendationReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link RecommendedBook} entities.
 * <p>
 * Why: queries precomputed personalised book recommendations with relevance scores and contextual reasons.
 */
@Repository
public interface RecommendedBookRepository extends JpaRepository<RecommendedBook, UUID>, JpaSpecificationExecutor<RecommendedBook> {

    /**
     * Finds active recommendations for a profile ordered by relevance score descending.
     */
    @Query("""
           SELECT r FROM RecommendedBook r
           JOIN FETCH r.book b
           WHERE r.profile.profileId = :profileId
             AND r.deletedAt IS NULL
           ORDER BY r.score DESC
           """)
    List<RecommendedBook> findActiveByProfileId(@Param("profileId") UUID profileId);

    /**
     * Pages active recommendations for a profile.
     */
    @Query(value = """
           SELECT r FROM RecommendedBook r
           JOIN FETCH r.book b
           WHERE r.profile.profileId = :profileId
             AND r.deletedAt IS NULL
           ORDER BY r.score DESC
           """,
           countQuery = """
           SELECT COUNT(r) FROM RecommendedBook r
           WHERE r.profile.profileId = :profileId
             AND r.deletedAt IS NULL
           """)
    Page<RecommendedBook> findActiveByProfileId(@Param("profileId") UUID profileId, Pageable pageable);

    /**
     * Finds recommendations filtered by a specific reason (e.g. AUTHOR_FOLLOW).
     */
    @Query("""
           SELECT r FROM RecommendedBook r
           JOIN FETCH r.book b
           WHERE r.profile.profileId = :profileId
             AND r.reason = :reason
             AND r.deletedAt IS NULL
           ORDER BY r.score DESC
           """)
    List<RecommendedBook> findActiveByProfileIdAndReason(
            @Param("profileId") UUID profileId,
            @Param("reason") RecommendationReason reason);

    /**
     * Finds a specific recommendation entry by profile and book.
     */
    @Query("""
           SELECT r FROM RecommendedBook r
           WHERE r.profile.profileId = :profileId
             AND r.book.bookId = :bookId
             AND r.deletedAt IS NULL
           """)
    Optional<RecommendedBook> findActiveByProfileAndBook(
            @Param("profileId") UUID profileId,
            @Param("bookId") UUID bookId);
}
