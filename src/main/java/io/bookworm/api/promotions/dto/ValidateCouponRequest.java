package io.bookworm.api.promotions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for validating coupon applicability at checkout.
 * <p>
 * Why: Accepts code and current order amount to calculate potential discount savings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotBlank(message = "Order amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Order amount must be a decimal string with up to 2 decimal places")
    private String orderAmount;
}
