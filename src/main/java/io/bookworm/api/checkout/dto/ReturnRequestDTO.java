package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Return request representation within an order.
 * <p>
 * Why: Transports return request state, reason, and estimated refund amount.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDTO {
    private UUID returnRequestId;
    private String status;
    private String reason;
    private MoneyDTO refundAmount;
    private OffsetDateTime requestedAt;
}
