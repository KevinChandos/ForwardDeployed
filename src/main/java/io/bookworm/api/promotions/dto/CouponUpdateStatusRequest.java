package io.bookworm.api.promotions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for toggling a coupon's active status.
 * <p>
 * Why: Enables promotional admins to activate or deactivate a coupon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUpdateStatusRequest {

    @NotNull(message = "isActive flag is required")
    private Boolean isActive;
}
