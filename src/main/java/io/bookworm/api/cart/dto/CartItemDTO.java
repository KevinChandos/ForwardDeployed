package io.bookworm.api.cart.dto;

import io.bookworm.api.catalogue.dto.BookSummaryDTO;
import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Item within a shopping cart.
 * <p>
 * Why: Transports book format details, unit price, quantity, and line subtotal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private UUID cartItemId;
    private BookSummaryDTO book;
    private UUID bookFormatId;
    private String formatType;
    private Integer quantity;
    private MoneyDTO unitPrice;
    private MoneyDTO subtotal;
}
