package io.bookworm.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for initiating an external payment transaction.
 * <p>
 * Why: Accepts order ID, payment method (CARD, NETBANKING, UPI, WALLET), and browser return redirect URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Return URL is required")
    private String returnUrl;
}
