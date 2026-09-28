package io.bookworm.api.cart.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Active shopping cart response DTO.
 * <p>
 * Why: Transports cart items, total itemCount, and calculated cart subtotal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private UUID cartId;
    private List<CartItemDTO> items;
    private Integer itemCount;
    private MoneyDTO subtotal;
}
