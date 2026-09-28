package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.BookFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link BookFormat} entities.
 */
@Repository
public interface BookFormatRepository extends JpaRepository<BookFormat, UUID> {

    /** Finds an active format belonging to a specific book. */
    @Query("""
           SELECT f FROM BookFormat f
           WHERE f.bookFormatId = :formatId
             AND f.book.bookId = :bookId
             AND f.deletedAt IS NULL
           """)
    Optional<BookFormat> findActiveByIdAndBookId(
            @Param("formatId") UUID formatId,
            @Param("bookId") UUID bookId);

    /** Returns all active formats for a book with their current prices. */
    @Query("""
           SELECT f FROM BookFormat f
           LEFT JOIN FETCH f.prices p
           WHERE f.book.bookId = :bookId
             AND f.isActive = true
             AND f.deletedAt IS NULL
             AND (p.effectiveTo IS NULL OR p.effectiveTo > CURRENT_TIMESTAMP)
           """)
    List<BookFormat> findActiveWithCurrentPricesByBookId(@Param("bookId") UUID bookId);
}
