package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Join entity assigning a Book to a Category.
 * <p>
 * Why: the Book–Category relationship is many-to-many; a full entity is used
 * (rather than a plain join table) so that the association itself can be
 * soft-deleted without removing either the book or the category, enabling
 * category re-tagging workflows with an audit trail.
 * <p>
 * Side effects: the composite unique constraint on {@code (book_id, category_id)}
 * is partial (WHERE deleted_at IS NULL), so the same book can be re-tagged to
 * a category after the previous assignment was soft-deleted.
 */
@Entity
@Table(
    schema = "catalogue",
    name = "book_categories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_book_categories_book_category",
                columnNames = {"book_id", "category_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookCategory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "book_category_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID bookCategoryId;

    /** The categorised book. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_book_categories_book"))
    private Book book;

    /** The assigned category. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_book_categories_category"))
    private Category category;
}
