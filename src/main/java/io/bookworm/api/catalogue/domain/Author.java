package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a book author in the catalogue bounded context.
 * <p>
 * Why: authors are first-class catalogue entities that can be followed by members
 * and linked to multiple books across different roles (AUTHOR, CO_AUTHOR, EDITOR,
 * TRANSLATOR). Separating them from the Book aggregate avoids data duplication
 * when the same author writes multiple books.
 * <p>
 * Side effects: {@code isActive = false} hides the author from public catalogue
 * listings but does not affect existing book–author relationships. Follower counts
 * are computed at query time from the author_follows table rather than cached here
 * to avoid write-amplification on every follow/unfollow.
 */
@Entity
@Table(schema = "catalogue", name = "authors")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Author extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "author_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID authorId;

    /** Full author name as it appears on book covers. */
    @Column(name = "name", nullable = false, length = 300)
    @ToString.Include
    private String name;

    /** Optional long-form biography (Markdown or plain text). */
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * URL of the author's profile photo. Stored as VARCHAR to keep media
     * in an external CDN; the DB only holds the reference.
     */
    @Column(name = "photo_url", length = 2000)
    private String photoUrl;

    /**
     * Whether this author appears in public catalogue listings.
     * Deactivating does not cascade to books; it only affects author search/browse.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Back-reference to all book–author join rows for this author. */
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookAuthor> bookAuthors = new ArrayList<>();
}
