package io.bookworm.api.checkout.domain;

import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.domain.BookFormat;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable line item snapshot for a confirmed Order.
 * <p>
 * Why: order lines capture the title, author credit, format, unit price, and
 * quantity at the exact moment the order is confirmed. This ensures that subsequent
 * catalogue edits (price changes, author renames) do not alter the financial record.
 * The {@code bookId} and {@code bookFormatId} are soft foreign keys — the order
 * remains valid even if the catalogue rows are later soft-deleted.
 * <p>
 * Side effects: {@code subtotal = unitPrice × quantity}. This is enforced by a
 * DB CHECK constraint and must also be validated in the service layer before insert.
 */
@Entity
@Table(schema = "ordering", name = "order_lines")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_line_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID orderLineId;

    /** The parent order. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_lines_order"))
    private Order order;

    /**
     * Soft reference to the catalogue Book. Preserved even if book is deleted.
     * Used for display and reporting; do not JOIN to live catalogue for order history.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_lines_book"))
    private Book book;

    /**
     * Soft reference to the catalogue BookFormat. Preserved even if format is deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_format_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_lines_book_format"))
    private BookFormat bookFormat;

    /** Immutable book title snapshot. */
    @Column(name = "title_snapshot", nullable = false, length = 500)
    private String titleSnapshot;

    /**
     * Immutable author display string snapshot (e.g. "J.K. Rowling, Mary GrandPré").
     */
    @Column(name = "author_snapshot", length = 500)
    private String authorSnapshot;

    /** Immutable format type snapshot (e.g. "PAPERBACK"). */
    @Column(name = "format_snapshot", nullable = false, length = 20)
    private String formatSnapshot;

    /** Number of copies. DB CHECK: quantity >= 1. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Per-unit price at order time. DB CHECK: unit_price > 0.
     * Stored as NUMERIC(14,2).
     */
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /**
     * unit_price × quantity. Stored for query convenience.
     * DB CHECK: subtotal = unit_price * quantity.
     */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";
}
