package io.bookworm.api.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for cancelling an order.
 * <p>
 * Why: Captures customer cancellation reason for audit and feedback logging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
