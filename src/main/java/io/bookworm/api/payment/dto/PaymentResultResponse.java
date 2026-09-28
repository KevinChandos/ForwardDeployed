package io.bookworm.api.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Result response following payment confirmation.
 * <p>
 * Why: Transports transaction status, internal order ID, and reference order number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultResponse {
    private UUID transactionId;
    private String status;
    private UUID orderId;
    private String orderNumber;
    private String failureReason;
}
