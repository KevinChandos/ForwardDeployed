package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Category} entities.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** Finds an active category by ID. */
    @Query("SELECT c FROM Category c WHERE c.categoryId = :id AND c.deletedAt IS NULL")
    Optional<Category> findActive(@Param("id") UUID id);

    /** Finds an active category by slug (for the /categories/by-slug/{slug} endpoint). */
    @Query("SELECT c FROM Category c WHERE c.slug = :slug AND c.deletedAt IS NULL")
    Optional<Category> findActiveBySlug(@Param("slug") String slug);

    /**
     * Returns all root categories (parent IS NULL) — the starting point for
     * building the full category tree.
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.deletedAt IS NULL ORDER BY c.name ASC")
    List<Category> findActiveRoots();

    /** Returns all active direct children of a given parent category. */
    @Query("""
           SELECT c FROM Category c
           WHERE c.parent.categoryId = :parentId
             AND c.deletedAt IS NULL
           ORDER BY c.name ASC
           """)
    List<Category> findActiveByParentId(@Param("parentId") UUID parentId);
}
