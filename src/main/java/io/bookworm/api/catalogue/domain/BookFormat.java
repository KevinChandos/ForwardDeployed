package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a specific format edition of a book (e.g. Paperback, Hardcover, Ebook).
 * <p>
 * Why: a single book title can be sold in multiple physical and digital formats,
 * each with its own ISBN, page count, and independent pricing history. Separating
 * formats from the Book aggregate allows per-format pricing and cart/wishlist
 * line items without duplicating book metadata.
 * <p>
 * Side effects: {@code isActive = false} hides the format from purchasable
 * listings but keeps historical price and order-line data intact. The unique
 * constraint on {@code (book_id, format_type)} is partial — only one active
 * format of each type is allowed per book.
 */
@Entity
@Table(
    schema = "catalogue",
    name = "book_formats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_book_formats_book_format_type",
                columnNames = {"book_id", "format_type"}),
        @UniqueConstraint(name = "uq_book_formats_isbn", columnNames = "isbn")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookFormat extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "book_format_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID bookFormatId;

    /** Parent book. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_book_formats_book"))
    private Book book;

    /**
     * Physical or digital format type. CHECK constraint at DB level.
     * Determines fulfilment behaviour: EBOOK is digital delivery, others are physical.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "format_type", nullable = false, length = 20)
    private FormatType formatType;

    /**
     * ISBN-13 (preferred) or ISBN-10. Unique when non-null across active formats.
     * NULL is allowed for older books or regional editions without an ISBN.
     */
    @Column(name = "isbn", length = 20)
    private String isbn;

    /**
     * Page count. Meaningful for PAPERBACK and HARDCOVER; typically null for EBOOK.
     */
    @Column(name = "page_count")
    private Integer pageCount;

    /** Whether this format is currently purchasable. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Price history for this format across stores. */
    @OneToMany(mappedBy = "bookFormat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookPrice> prices = new ArrayList<>();

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Format types as defined in the OpenAPI spec. */
    public enum FormatType {
        PAPERBACK,
        HARDCOVER,
        EBOOK
    }
}
