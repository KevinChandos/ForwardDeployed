package io.bookworm.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for confirming payment from gateway webhook or client callback.
 * <p>
 * Why: Captures gateway payment ID, signature, and status for verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {

    @NotBlank(message = "Gateway payment ID is required")
    private String gatewayPaymentId;

    private String gatewaySignature;

    @NotBlank(message = "Status is required")
    private String status;
}
