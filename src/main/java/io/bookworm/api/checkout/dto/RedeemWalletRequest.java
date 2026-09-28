package io.bookworm.api.checkout.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for redeeming wallet reward points during checkout.
 * <p>
 * Why: Accepts points amount to deduct from total order amount.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemWalletRequest {

    @NotNull(message = "Points to redeem is required")
    @DecimalMin(value = "0.01", message = "Points must be strictly positive")
    private BigDecimal pointsToRedeem;
}
