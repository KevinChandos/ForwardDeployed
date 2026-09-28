package io.bookworm.api.recommendation.domain;

import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single personalised book recommendation entry within a RecommendationProfile.
 * <p>
 * Why: recommendation scores are pre-computed (batch or event-driven) and stored
 * here so that the hot-path recommendation API is a simple indexed SELECT rather
 * than a live scoring computation. The {@code reason} field enables the frontend
 * to render contextual labels ("Because you ordered X" / "Follows Y author").
 * <p>
 * Side effects: the partial unique constraint on {@code (profile_id, book_id)}
 * prevents duplicate recommendations per profile while allowing re-addition after
 * soft-delete. Score updates to existing rows use optimistic locking.
 */
@Entity
@Table(
    schema = "discovery",
    name = "recommended_books",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_recommended_books_profile_book",
                columnNames = {"profile_id", "book_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecommendedBook extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID entryId;

    /** The profile this recommendation belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_recommended_books_profile"))
    private RecommendationProfile profile;

    /** The recommended book. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_recommended_books_book"))
    private Book book;

    /**
     * Relevance score in range 0.0000–1.0000.
     * Higher values appear first in recommendation lists.
     */
    @Column(name = "score", nullable = false, precision = 6, scale = 4)
    private BigDecimal score;

    /**
     * Human-interpretable reason for this recommendation.
     * Maps to enum values; DB CHECK constraint enforces allowed values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private RecommendationReason reason;

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Why this book was recommended to the profile's owner. */
    public enum RecommendationReason {
        ORDER_HISTORY,
        CATEGORY_AFFINITY,
        AUTHOR_FOLLOW
    }
}
