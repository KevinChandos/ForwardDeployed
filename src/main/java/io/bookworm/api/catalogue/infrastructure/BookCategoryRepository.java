package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.BookCategory;
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
 * JPA repository for {@link BookCategory} join entities.
 * <p>
 * Why: queries and manages categories assigned to books.
 */
@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, UUID>, JpaSpecificationExecutor<BookCategory> {

    /**
     * Finds all active category assignments for a book.
     */
    @Query("""
           SELECT bc FROM BookCategory bc
           JOIN FETCH bc.category c
           WHERE bc.book.bookId = :bookId
             AND bc.deletedAt IS NULL
             AND c.deletedAt IS NULL
           """)
    List<BookCategory> findActiveByBookId(@Param("bookId") UUID bookId);

    /**
     * Pages all books assigned to a category.
     */
    @Query(value = """
           SELECT bc FROM BookCategory bc
           JOIN FETCH bc.book b
           WHERE bc.category.categoryId = :categoryId
             AND bc.deletedAt IS NULL
             AND b.deletedAt IS NULL
           """,
           countQuery = """
           SELECT COUNT(bc) FROM BookCategory bc
           WHERE bc.category.categoryId = :categoryId
             AND bc.deletedAt IS NULL
           """)
    Page<BookCategory> findActiveByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Finds a specific book-category assignment.
     */
    @Query("""
           SELECT bc FROM BookCategory bc
           WHERE bc.book.bookId = :bookId
             AND bc.category.categoryId = :categoryId
             AND bc.deletedAt IS NULL
           """)
    Optional<BookCategory> findActiveByBookAndCategory(
            @Param("bookId") UUID bookId,
            @Param("categoryId") UUID categoryId);
}
