package io.bookworm.api.promotions.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Request payload for updating a coupon.
 * <p>
 * Why: Allows partial update of description, max usage cap, expiration date, and version for optimistic locking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponRequest {

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private OffsetDateTime expiresAt;

    @NotNull(message = "Version is required for optimistic locking")
    @Min(value = 1, message = "Version must be at least 1")
    private Integer version;
}
