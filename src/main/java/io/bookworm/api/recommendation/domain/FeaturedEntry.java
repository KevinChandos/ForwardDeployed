package io.bookworm.api.recommendation.domain;

import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A single ranked book entry within a FeaturedList.
 * <p>
 * Why: the rank column drives display order on the storefront. Using a full entity
 * (rather than a join-table with just IDs) allows the rank to be edited without
 * removing and re-adding entries. The pair unique constraints prevent the same book
 * appearing twice in a list and enforce unique rank positions.
 * <p>
 * Side effects: inserting a new entry at a rank that already exists requires the
 * application to shift existing ranks — the DB does not do this automatically.
 * DB CHECK: rank >= 1.
 */
@Entity
@Table(
    schema = "discovery",
    name = "featured_entries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_featured_entries_list_book",
                columnNames = {"list_id", "book_id"}),
        @UniqueConstraint(name = "uq_featured_entries_list_rank",
                columnNames = {"list_id", "rank"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FeaturedEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID entryId;

    /** The list this entry belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_featured_entries_list"))
    private FeaturedList featuredList;

    /** The featured book. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_featured_entries_book"))
    private Book book;

    /**
     * 1-based display rank. Must be unique within the list.
     * DB CHECK: rank >= 1.
     */
    @Column(name = "rank", nullable = false)
    private Integer rank;
}
