package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A hierarchical book category (self-referencing adjacency list tree).
 * <p>
 * Why: categories form an arbitrary-depth tree (e.g. Fiction → Science Fiction
 * → Cyberpunk). The adjacency list model is the simplest approach for a moderate
 * tree depth — alternative nested sets would complicate inserts/updates. Tree
 * queries use recursive CTEs in PostgreSQL for performance when depth is needed.
 * <p>
 * Side effects: deleting a parent category must be blocked at the application
 * layer if child categories still reference it. The self-FK
 * ({@code parent_category_id}) is nullable — null means a root category.
 */
@Entity
@Table(
    schema = "catalogue",
    name = "categories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_categories_slug", columnNames = "slug"),
        @UniqueConstraint(name = "uq_categories_name", columnNames = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID categoryId;

    /** Human-readable category name (unique among active categories). */
    @Column(name = "name", nullable = false, length = 150)
    @ToString.Include
    private String name;

    /**
     * URL-safe lowercase-hyphenated slug (e.g. "science-fiction").
     * Used in path segments: {@code GET /categories/by-slug/{slug}}.
     */
    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    /**
     * Parent category. NULL indicates a root-level category.
     * Self-join enables the adjacency list tree navigation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id",
                foreignKey = @ForeignKey(name = "fk_categories_parent"))
    private Category parent;

    /** Direct children of this category node. */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    /** Back-reference to book–category assignments. */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookCategory> bookCategories = new ArrayList<>();
}
