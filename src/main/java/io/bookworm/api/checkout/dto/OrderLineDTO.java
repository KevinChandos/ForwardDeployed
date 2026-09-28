package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Line item representation within a confirmed order.
 * <p>
 * Why: Transports immutable book title, author, format snapshots, unit price, quantity, and line total.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineDTO {
    private UUID orderLineId;
    private UUID bookId;
    private String title;
    private String author;
    private String formatType;
    private Integer quantity;
    private MoneyDTO unitPrice;
    private MoneyDTO subtotal;
}
