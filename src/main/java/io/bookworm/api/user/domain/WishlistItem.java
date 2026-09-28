package io.bookworm.api.user.domain;

import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.domain.BookFormat;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single saved item in a member's Wishlist.
 * <p>
 * Why: wishlist items track the specific format (PAPERBACK, HARDCOVER, EBOOK) the
 * member wants, not just the book, because pricing and availability differ per format.
 * The unique constraint on {@code (wishlist_id, book_format_id)} prevents saving
 * the same format twice.
 * <p>
 * Side effects: soft-deleting this row removes the item from the wishlist view.
 * Moving an item to the cart does NOT automatically remove it from the wishlist.
 */
@Entity
@Table(
    schema = "cart",
    name = "wishlist_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_wishlist_items_wishlist_format",
                columnNames = {"wishlist_id", "book_format_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WishlistItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wishlist_item_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID wishlistItemId;

    /** The parent wishlist. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wishlist_items_wishlist"))
    private Wishlist wishlist;

    /** The book (for display metadata like title and cover image). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wishlist_items_book"))
    private Book book;

    /** The specific format edition the member wants. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_format_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wishlist_items_book_format"))
    private BookFormat bookFormat;

    /** Timestamp when the item was added to the wishlist. */
    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();
}
