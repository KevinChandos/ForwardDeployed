package io.bookworm.api.catalogue.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import io.bookworm.api.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Time-bounded price record for a BookFormat in a specific Store.
 * <p>
 * Why: prices are not directly embedded in BookFormat because they are
 * store-specific and time-bounded. The design supports price history — when a
 * price changes, the previous row's {@code effectiveTo} is set and a new row is
 * inserted. The current price is the row where {@code effectiveTo IS NULL}
 * for a given (book_format_id, store_id) pair.
 * <p>
 * Side effects: {@code amount} is stored as NUMERIC(14,2) and mapped to BigDecimal
 * to prevent IEEE-754 precision errors. Both {@code effectiveFrom} and
 * {@code effectiveTo} must be checked for range validity — the DB CHECK constraint
 * enforces {@code effective_to IS NULL OR effective_to > effective_from}.
 */
@Entity
@Table(schema = "catalogue", name = "book_prices")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookPrice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "book_price_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID bookPriceId;

    /** The format this price applies to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_format_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_book_prices_book_format"))
    private BookFormat bookFormat;

    /**
     * The store this price is valid in. Cross-context FK to store.stores.
     * Lazy because the store is rarely needed when reading catalogue prices.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_book_prices_store"))
    private Store store;

    /**
     * Price amount. Must be strictly positive (DB CHECK: amount > 0).
     * Stored as NUMERIC(14,2).
     */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /**
     * ISO-4217 currency code (e.g. "INR").
     * Must match the store's configured currency.
     */
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    /**
     * Start of the price validity window. Defaults to insertion time.
     */
    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom = OffsetDateTime.now();

    /**
     * End of the price validity window. NULL means this is the current price.
     * Must be after {@code effectiveFrom} when non-null (DB CHECK constraint).
     */
    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;
}
