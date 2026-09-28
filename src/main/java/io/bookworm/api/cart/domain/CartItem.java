package io.bookworm.api.cart.domain;

import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.domain.BookFormat;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single line item in a shopping cart.
 * <p>
 * Why: cart items reference both the Book (for display metadata) and the BookFormat
 * (for the specific edition being purchased). The {@code unitPrice} snapshot is
 * taken at the time the item is added to the cart to prevent price changes from
 * silently affecting the cart — the service recalculates and warns when the
 * current price differs from the snapshot during checkout.
 * <p>
 * Side effects: soft-deleting a CartItem represents item removal from the cart.
 * The unique constraint on {@code (cart_id, book_format_id)} prevents duplicate
 * format entries; adding the same format again updates quantity instead.
 */
@Entity
@Table(
    schema = "cart",
    name = "cart_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_cart_items_cart_format",
                columnNames = {"cart_id", "book_format_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CartItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cart_item_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID cartItemId;

    /** Parent cart. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cart_items_cart"))
    private Cart cart;

    /** The book being added to the cart (used for display metadata). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cart_items_book"))
    private Book book;

    /** Specific format edition being purchased. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_format_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cart_items_book_format"))
    private BookFormat bookFormat;

    /**
     * Number of copies. DB CHECK: quantity >= 1.
     * API spec caps at 10 copies per item.
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    /**
     * Price per unit at the time the item was added.
     * Snapshot to detect price drift during checkout. DB CHECK: unit_price > 0.
     */
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** Currency of the unit price (ISO-4217). */
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    /** Timestamp when this item was added to the cart. */
    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();
}
