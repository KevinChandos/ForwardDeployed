package io.bookworm.api.recommendation.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import io.bookworm.api.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A named, time-bounded list of featured books (e.g. Bestsellers, New Launches).
 * <p>
 * Why: featured lists are editorial content managed by store admins. Separating
 * them from the algorithmic recommendation profiles allows independent curation
 * workflows. The {@code effectiveTo} field lets admins schedule future lists
 * without taking the current one offline first.
 * <p>
 * Side effects: the service layer selects the active list where
 * {@code effectiveTo IS NULL OR effectiveTo > now()} for a given
 * {@code (store_id, list_type)} pair.
 */
@Entity
@Table(schema = "discovery", name = "featured_lists")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FeaturedList extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "list_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID listId;

    /** The store this list belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_featured_lists_store"))
    private Store store;

    /**
     * Type of list. DB CHECK enforces allowed values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 30)
    private ListType listType;

    /** When this list became / becomes active. */
    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom = OffsetDateTime.now();

    /** When this list expires. NULL means currently active with no set end date. */
    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    /** Ordered entries in this list. */
    @OneToMany(mappedBy = "featuredList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("rank ASC")
    private List<FeaturedEntry> entries = new ArrayList<>();

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Allowed list types as defined in the DB CHECK constraint. */
    public enum ListType {
        BESTSELLER,
        NEW_LAUNCH,
        RECOMMENDED
    }
}
