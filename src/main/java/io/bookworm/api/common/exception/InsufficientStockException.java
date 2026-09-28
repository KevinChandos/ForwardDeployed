package io.bookworm.api.common.exception;

/**
 * Thrown when an item cannot be added or purchased because the available stock is insufficient.
 * <p>
 * Why: Subclasses {@link BusinessRuleException} so the global handler maps it to HTTP 422
 * with a distinct {@code INSUFFICIENT_STOCK} error code, allowing clients to present a specific
 * "out of stock" message rather than a generic business-rule error.
 * <p>
 * Side effects: Triggers transaction rollback; the catalogue write that caused the check is
 * rolled back before this exception reaches the handler.
 */
public class InsufficientStockException extends BusinessRuleException {

    private final String bookId;
    private final int requested;
    private final int available;

    public InsufficientStockException(String bookId, int requested, int available) {
        super("INSUFFICIENT_STOCK",
                String.format("Insufficient stock for book '%s': requested %d, available %d",
                        bookId, requested, available));
        this.bookId = bookId;
        this.requested = requested;
        this.available = available;
    }

    public String getBookId()   { return bookId; }
    public int    getRequested() { return requested; }
    public int    getAvailable() { return available; }
}
