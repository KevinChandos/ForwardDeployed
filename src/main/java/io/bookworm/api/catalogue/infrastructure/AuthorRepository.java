package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Author} entities.
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, UUID> {

    /** Finds an active author by ID. */
    @Query("SELECT a FROM Author a WHERE a.authorId = :id AND a.deletedAt IS NULL")
    Optional<Author> findActive(@Param("id") UUID id);

    /** Pages through all active authors. */
    @Query(value = "SELECT a FROM Author a WHERE a.deletedAt IS NULL",
           countQuery = "SELECT COUNT(a) FROM Author a WHERE a.deletedAt IS NULL")
    Page<Author> findAllActive(Pageable pageable);

    /**
     * Counts active followers for an author.
     * Used to populate the followerCount field in AuthorDetailResponse.
     */
    @Query("""
           SELECT COUNT(f) FROM AuthorFollow f
           WHERE f.author.authorId = :authorId
             AND f.deletedAt IS NULL
           """)
    long countActiveFollowers(@Param("authorId") UUID authorId);
}
