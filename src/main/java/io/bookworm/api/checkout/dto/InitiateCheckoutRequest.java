package io.bookworm.api.checkout.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for starting a checkout session.
 * <p>
 * Why: Accepts store ID and optional cart ID to initialise the checkout session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCheckoutRequest {

    @NotNull(message = "Store ID is required")
    private UUID storeId;

    private UUID cartId;
}
