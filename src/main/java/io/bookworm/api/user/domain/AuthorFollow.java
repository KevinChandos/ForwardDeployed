package io.bookworm.api.user.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.catalogue.domain.Author;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records a follow relationship between a Member and an Author.
 * <p>
 * Why: follow relationships are many-to-many but carry metadata ({@code followedAt})
 * that prevents a plain join table from being sufficient. Using a full entity lets
 * the application query "all authors followed by a member" efficiently via an index
 * on {@code (member_id)} while keeping the cross-bounded-context FK at the DB level.
 * <p>
 * Side effects: unfollowing is implemented as a soft-delete (setting {@code deletedAt}).
 * The composite unique constraint on {@code (member_id, author_id)} is partial — only
 * active (non-deleted) follows are constrained, allowing a member to re-follow after
 * unfollowing.
 */
@Entity
@Table(
    schema = "identity",
    name = "author_follows",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_author_follows_member_author",
                columnNames = {"member_id", "author_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorFollow extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "follow_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID followId;

    /** The member who is following the author. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_author_follows_member"))
    private Member member;

    /**
     * The author being followed. Cross-context FK to catalogue.authors.
     * Lazy loading is intentional — the author is only needed when rendering
     * the "following" list, not during follow/unfollow operations.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_author_follows_author"))
    private Author author;

    /** Timestamp when the member first followed the author. */
    @Column(name = "followed_at", nullable = false)
    private OffsetDateTime followedAt = OffsetDateTime.now();
}
