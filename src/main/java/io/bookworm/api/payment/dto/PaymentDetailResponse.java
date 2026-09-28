package io.bookworm.api.payment.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detailed payment transaction response representation.
 * <p>
 * Why: Transports full transaction details, gateway reference codes, timestamps, and failure explanations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailResponse {
    private UUID transactionId;
    private UUID orderId;
    private MoneyDTO amount;
    private String paymentMethod;
    private String status;
    private String gatewayTransactionId;
    private OffsetDateTime initiatedAt;
    private OffsetDateTime completedAt;
    private String failureReason;
}
