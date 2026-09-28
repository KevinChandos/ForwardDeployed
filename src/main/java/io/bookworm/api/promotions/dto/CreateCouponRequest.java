package io.bookworm.api.promotions.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request payload for creating a promo coupon.
 * <p>
 * Why: Validates discount type (FLAT/PERCENT), numeric discount value, usage caps, and expiration date.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {

    @NotNull(message = "Store ID is required")
    private UUID storeId;

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must contain only uppercase alphanumeric characters and hyphens")
    private String code;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Discount type is required")
    @Pattern(regexp = "^(FLAT|PERCENT)$", message = "Discount type must be FLAT or PERCENT")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be strictly positive")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Min order amount must be non-negative")
    private BigDecimal minOrderAmount;

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private OffsetDateTime expiresAt;
}
