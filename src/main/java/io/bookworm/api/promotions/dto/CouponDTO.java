package io.bookworm.api.promotions.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Coupon representation DTO.
 * <p>
 * Why: Transports discount rules, usage counters, expiration, and active status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {
    private UUID couponId;
    private String code;
    private String description;
    private String discountType;
    private Double discountValue;
    private MoneyDTO minOrderAmount;
    private Integer maxUses;
    private Integer usedCount;
    private OffsetDateTime expiresAt;
    private Boolean isActive;
}
