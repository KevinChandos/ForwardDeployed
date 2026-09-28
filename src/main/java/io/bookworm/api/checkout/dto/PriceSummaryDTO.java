package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Breakdown of item subtotal, delivery charges, discounts, taxes, and final payable total.
 * <p>
 * Why: Standardised price calculation summary shared by checkout sessions and orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceSummaryDTO {
    private MoneyDTO itemsSubtotal;
    private MoneyDTO shippingCost;
    private MoneyDTO couponDiscount;
    private MoneyDTO walletDiscount;
    private MoneyDTO taxAmount;
    private MoneyDTO totalPayable;
}
