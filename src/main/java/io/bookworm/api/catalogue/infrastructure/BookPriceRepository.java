package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.BookPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link BookPrice} entities.
 * <p>
 * Why: retrieves store-specific and time-bounded price records for book formats.
 */
@Repository
public interface BookPriceRepository extends JpaRepository<BookPrice, UUID>, JpaSpecificationExecutor<BookPrice> {

    /**
     * Finds the current price for a format in a specific store.
     * Current price is valid where effectiveFrom <= time and (effectiveTo IS NULL or effectiveTo > time).
     */
    @Query("""
           SELECT p FROM BookPrice p
           WHERE p.bookFormat.bookFormatId = :formatId
             AND p.store.storeId = :storeId
             AND p.effectiveFrom <= :now
             AND (p.effectiveTo IS NULL OR p.effectiveTo > :now)
             AND p.deletedAt IS NULL
           """)
    Optional<BookPrice> findCurrentPrice(
            @Param("formatId") UUID formatId,
            @Param("storeId") UUID storeId,
            @Param("now") OffsetDateTime now);

    /**
     * Finds all current prices for all formats of a book in a store.
     */
    @Query("""
           SELECT p FROM BookPrice p
           JOIN FETCH p.bookFormat f
           WHERE f.book.bookId = :bookId
             AND p.store.storeId = :storeId
             AND p.effectiveFrom <= :now
             AND (p.effectiveTo IS NULL OR p.effectiveTo > :now)
             AND p.deletedAt IS NULL
             AND f.deletedAt IS NULL
           """)
    List<BookPrice> findCurrentPricesForBook(
            @Param("bookId") UUID bookId,
            @Param("storeId") UUID storeId,
            @Param("now") OffsetDateTime now);

    /**
     * Pages the price history for a format in a store.
     */
    @Query(value = """
           SELECT p FROM BookPrice p
           WHERE p.bookFormat.bookFormatId = :formatId
             AND p.store.storeId = :storeId
             AND p.deletedAt IS NULL
           ORDER BY p.effectiveFrom DESC
           """,
           countQuery = """
           SELECT COUNT(p) FROM BookPrice p
           WHERE p.bookFormat.bookFormatId = :formatId
             AND p.store.storeId = :storeId
             AND p.deletedAt IS NULL
           """)
    Page<BookPrice> findPriceHistory(
            @Param("formatId") UUID formatId,
            @Param("storeId") UUID storeId,
            Pageable pageable);
}
