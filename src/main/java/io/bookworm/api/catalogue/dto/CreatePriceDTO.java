package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Price definition DTO for book format creation.
 * <p>
 * Why: Captures store-specific amount and currency when initializing a format's pricing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePriceDTO {

    @NotNull(message = "Store ID is required")
    private UUID storeId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly positive")
    private BigDecimal amount;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;
}
