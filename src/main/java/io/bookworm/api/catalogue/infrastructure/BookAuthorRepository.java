package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.BookAuthor;
import io.bookworm.api.catalogue.domain.BookAuthor.AuthorRole;
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
 * JPA repository for {@link BookAuthor} join entities.
 * <p>
 * Why: queries and manages author credits and roles associated with books.
 */
@Repository
public interface BookAuthorRepository extends JpaRepository<BookAuthor, UUID>, JpaSpecificationExecutor<BookAuthor> {

    /**
     * Finds all active author credits for a specific book.
     */
    @Query("""
           SELECT ba FROM BookAuthor ba
           JOIN FETCH ba.author a
           WHERE ba.book.bookId = :bookId
             AND ba.deletedAt IS NULL
             AND a.deletedAt IS NULL
           """)
    List<BookAuthor> findActiveByBookId(@Param("bookId") UUID bookId);

    /**
     * Pages all books credited to a specific author.
     */
    @Query(value = """
           SELECT ba FROM BookAuthor ba
           JOIN FETCH ba.book b
           WHERE ba.author.authorId = :authorId
             AND ba.deletedAt IS NULL
             AND b.deletedAt IS NULL
           """,
           countQuery = """
           SELECT COUNT(ba) FROM BookAuthor ba
           WHERE ba.author.authorId = :authorId
             AND ba.deletedAt IS NULL
           """)
    Page<BookAuthor> findActiveByAuthorId(@Param("authorId") UUID authorId, Pageable pageable);

    /**
     * Finds a specific author credit assignment by book, author, and role.
     */
    @Query("""
           SELECT ba FROM BookAuthor ba
           WHERE ba.book.bookId = :bookId
             AND ba.author.authorId = :authorId
             AND ba.role = :role
             AND ba.deletedAt IS NULL
           """)
    Optional<BookAuthor> findActiveByBookAndAuthorAndRole(
            @Param("bookId") UUID bookId,
            @Param("authorId") UUID authorId,
            @Param("role") AuthorRole role);
}
