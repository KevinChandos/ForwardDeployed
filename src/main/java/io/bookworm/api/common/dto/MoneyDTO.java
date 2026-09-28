package io.bookworm.api.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Monetary DTO representing a decimal amount and ISO-4217 currency code.
 * <p>
 * Why: Amount is mapped as a string with regex constraint to prevent IEEE-754 floating-point inaccuracies
 * across JSON serialization boundaries as mandated by the OpenAPI specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyDTO {

    @NotBlank(message = "Amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Amount must be a valid decimal string with up to 2 decimal places")
    private String amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be an ISO-4217 3-letter code")
    private String currency;
}
