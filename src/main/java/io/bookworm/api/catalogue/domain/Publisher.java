package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a book publisher in the catalogue bounded context.
 * <p>
 * Why: publishers are reusable catalogue entities shared across many books.
 * Storing them as a separate entity avoids denormalising the publisher name into
 * every book row and enables publisher-level browsing endpoints.
 * <p>
 * Side effects: {@code isActive = false} removes the publisher from public
 * publisher listings but does NOT cascade to books — books published by an
 * inactive publisher remain visible in the catalogue.
 */
@Entity
@Table(
    schema = "catalogue",
    name = "publishers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_publishers_name", columnNames = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Publisher extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "publisher_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID publisherId;

    /** Publisher name — must be unique among active publishers. */
    @Column(name = "name", nullable = false, length = 300)
    @ToString.Include
    private String name;

    /**
     * Publisher website URL. Stored as a plain URL string; the application layer
     * validates the URI format before persistence.
     */
    @Column(name = "website", length = 2000)
    private String website;

    /** Whether this publisher appears in public listings. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Books associated with this publisher. */
    @OneToMany(mappedBy = "publisher", fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();
}
