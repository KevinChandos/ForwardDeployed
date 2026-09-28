package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The primary Book aggregate root in the catalogue bounded context.
 * <p>
 * Why: Book is the central entity around which catalogue, search, reviews,
 * cart, and ordering revolve. Denormalised counters ({@code salesCount},
 * {@code averageRating}, {@code reviewCount}) are maintained via domain events
 * to avoid expensive aggregate queries on hot read paths. They are updated
 * by the ReviewPublished and OrderDelivered events respectively.
 * <p>
 * Side effects: {@code isActive = false} removes the book from public listings.
 * The book's formats and prices remain in the database for historical order line
 * traceability. Optimistic locking applies — concurrent admin edits will
 * produce an OPTIMISTIC_LOCK_CONFLICT (HTTP 409).
 */
@Entity
@Table(schema = "catalogue", name = "books")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Book extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "book_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID bookId;

    /** Full book title. Max 500 chars per API spec. */
    @Column(name = "title", nullable = false, length = 500)
    @ToString.Include
    private String title;

    /** Optional long-form synopsis (Markdown or plain text). */
    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    /** Publication language e.g. "English", "Hindi". */
    @Column(name = "language", nullable = false, length = 50)
    private String language;

    /**
     * Cover image URL (CDN reference, not inline data).
     * Stored as VARCHAR to avoid BLOB performance issues.
     */
    @Column(name = "cover_image_url", length = 2000)
    private String coverImageUrl;

    /** Original publication date. May be null for unpublished titles. */
    @Column(name = "published_date")
    private LocalDate publishedDate;

    /**
     * Denormalised sales counter. Updated asynchronously via OrderDelivered event.
     * Must not be used for inventory management — it is informational only.
     */
    @Column(name = "sales_count", nullable = false)
    private Integer salesCount = 0;

    /**
     * Denormalised average star rating (1.00–5.00). Updated by ReviewPublished
     * and ReviewUnpublished events. NULL when no published reviews exist.
     */
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    /**
     * Denormalised count of published reviews. Updated alongside averageRating
     * to avoid a COUNT(*) query on the review table during catalogue reads.
     */
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    /**
     * Optional publisher reference. Nullable because not all books have a
     * publisher record in the system (e.g. self-published works).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id",
                foreignKey = @ForeignKey(name = "fk_books_publisher"))
    private Publisher publisher;

    /**
     * Whether this book is publicly visible in the catalogue.
     * Deactivation does NOT delete book data or affect existing orders.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Physical and digital format editions of this book. */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookFormat> formats = new ArrayList<>();

    /** Author assignments with roles. */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookAuthor> bookAuthors = new ArrayList<>();

    /** Category assignments. */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookCategory> bookCategories = new ArrayList<>();
}
