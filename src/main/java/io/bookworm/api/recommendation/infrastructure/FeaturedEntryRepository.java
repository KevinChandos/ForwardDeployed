package io.bookworm.api.recommendation.infrastructure;

import io.bookworm.api.recommendation.domain.FeaturedEntry;
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
 * JPA repository for {@link FeaturedEntry} entities.
 * <p>
 * Why: accesses ranked entries within featured lists.
 */
@Repository
public interface FeaturedEntryRepository extends JpaRepository<FeaturedEntry, UUID>, JpaSpecificationExecutor<FeaturedEntry> {

    /**
     * Finds all active entries in a featured list ordered by display rank ascending.
     */
    @Query("""
           SELECT e FROM FeaturedEntry e
           JOIN FETCH e.book b
           WHERE e.featuredList.listId = :listId
             AND e.deletedAt IS NULL
           ORDER BY e.rank ASC
           """)
    List<FeaturedEntry> findActiveByListId(@Param("listId") UUID listId);

    /**
     * Finds an entry for a specific book in a list.
     */
    @Query("""
           SELECT e FROM FeaturedEntry e
           WHERE e.featuredList.listId = :listId
             AND e.book.bookId = :bookId
             AND e.deletedAt IS NULL
           """)
    Optional<FeaturedEntry> findActiveByListAndBook(
            @Param("listId") UUID listId,
            @Param("bookId") UUID bookId);

    /**
     * Pages all entries in a list.
     */
    @Query(value = """
           SELECT e FROM FeaturedEntry e
           WHERE e.featuredList.listId = :listId
             AND e.deletedAt IS NULL
           ORDER BY e.rank ASC
           """,
           countQuery = """
           SELECT COUNT(e) FROM FeaturedEntry e
           WHERE e.featuredList.listId = :listId
             AND e.deletedAt IS NULL
           """)
    Page<FeaturedEntry> findActiveByListId(@Param("listId") UUID listId, Pageable pageable);
}
