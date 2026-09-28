package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for updating the price of a format in a store.
 * <p>
 * Why: Sets the price for a format in a specific store, closing the previous price validity period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePriceRequest {

    @NotNull(message = "Store ID is required")
    private UUID storeId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly positive")
    private BigDecimal amount;
}
