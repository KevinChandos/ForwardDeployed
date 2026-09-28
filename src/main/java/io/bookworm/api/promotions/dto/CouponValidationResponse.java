package io.bookworm.api.promotions.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result response after coupon validation calculation.
 * <p>
 * Why: Transports validation status, discount type, computed discount savings, and rejection reason if invalid.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    private Boolean isValid;
    private String couponCode;
    private String discountType;
    private MoneyDTO discountAmount;
    private String invalidReason;
}
