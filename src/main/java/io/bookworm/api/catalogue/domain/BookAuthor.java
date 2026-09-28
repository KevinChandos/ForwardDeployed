package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Join entity linking a Book to an Author with an explicit role.
 * <p>
 * Why: the relationship between Book and Author is many-to-many with a
 * discriminator (role). A simple JPA join table cannot carry the role column,
 * so this full entity is required. The composite unique constraint on
 * {@code (book_id, author_id, role)} prevents duplicate role assignments.
 * <p>
 * Side effects: if an author is deactivated, their existing BookAuthor rows
 * remain intact — books still show the author credit at the DB level. The
 * service layer must decide whether to include inactive authors in API responses.
 */
@Entity
@Table(
    schema = "catalogue",
    name = "book_authors",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_book_authors_book_author_role",
                columnNames = {"book_id", "author_id", "role"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookAuthor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "book_author_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID bookAuthorId;

    /** The book this credit belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_book_authors_book"))
    private Book book;

    /** The credited author. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_book_authors_author"))
    private Author author;

    /**
     * Role of the author on this specific book.
     * Stored as VARCHAR; CHECK constraint enforces allowed values at DB level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private AuthorRole role = AuthorRole.AUTHOR;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** All valid author roles as defined in the OpenAPI spec. */
    public enum AuthorRole {
        AUTHOR,
        CO_AUTHOR,
        EDITOR,
        TRANSLATOR
    }
}
