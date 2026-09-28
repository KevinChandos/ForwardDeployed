package io.bookworm.api.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for applying a promo coupon to a checkout session.
 * <p>
 * Why: Accepts uppercase coupon code string to calculate and apply discounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;
}
