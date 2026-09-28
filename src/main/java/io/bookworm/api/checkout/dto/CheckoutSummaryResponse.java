package io.bookworm.api.checkout.dto;

import io.bookworm.api.cart.dto.CartItemDTO;
import io.bookworm.api.user.dto.AddressDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Full checkout summary response DTO.
 * <p>
 * Why: Bundles items, shipping address, coupon, wallet redemptions, and price breakdown prior to payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSummaryResponse {
    private UUID sessionId;
    private List<CartItemDTO> items;
    private AddressDTO deliveryAddress;
    private CouponSummaryDTO appliedCoupon;
    private PriceSummaryDTO priceSummary;
}
