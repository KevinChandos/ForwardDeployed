package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payment summary DTO for order views.
 * <p>
 * Why: Conveys payment status, method, and transaction reference.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDTO {
    private UUID transactionId;
    private String status;
    private String paymentMethod;
    private MoneyDTO amount;
}
