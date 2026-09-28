package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Book} entities.
 * <p>
 * Why: {@link JpaSpecificationExecutor} is included so that the search service can
 * compose dynamic WHERE clauses (category filter, language filter, price range, etc.)
 * via the JPA Criteria API without hand-written JPQL for every combination.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    /** Finds a single active book by ID, with formats and authors eagerly loaded. */
    @Query("""
           SELECT b FROM Book b
           LEFT JOIN FETCH b.formats f
           LEFT JOIN FETCH b.bookAuthors ba
           LEFT JOIN FETCH ba.author
           WHERE b.bookId = :id AND b.deletedAt IS NULL
           """)
    Optional<Book> findActiveWithDetails(@Param("id") UUID id);

    /** Pages through all active books. */
    @Query(value = "SELECT b FROM Book b WHERE b.isActive = true AND b.deletedAt IS NULL",
           countQuery = "SELECT COUNT(b) FROM Book b WHERE b.isActive = true AND b.deletedAt IS NULL")
    Page<Book> findAllActive(Pageable pageable);
}
