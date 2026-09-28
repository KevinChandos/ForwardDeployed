package io.bookworm.api.payment.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response payload following payment initiation.
 * <p>
 * Why: Returns the internal transaction ID, gateway payment order ID, and the gateway redirection URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiatedResponse {
    private UUID transactionId;
    private String gatewayOrderId;
    private String paymentUrl;
    private MoneyDTO amount;
    private String status;
}
