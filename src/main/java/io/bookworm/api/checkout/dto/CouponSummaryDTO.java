package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Summary coupon DTO used across checkout and order confirmation.
 * <p>
 * Why: Summarises the applied coupon code, discount amount, and promotional rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSummaryDTO {
    private UUID couponId;
    private String code;
    private String discountType;
    private MoneyDTO discountAmount;
}
