package io.bookworm.api.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for initiating an order return.
 * <p>
 * Why: Accepts return reason and optional item context for return authorization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestBody {

    @NotBlank(message = "Return reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}
