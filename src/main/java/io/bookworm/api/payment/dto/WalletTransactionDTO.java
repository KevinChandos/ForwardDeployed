package io.bookworm.api.payment.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transaction history log entry for a member's reward wallet account.
 * <p>
 * Why: Transports transaction type (CREDIT, DEBIT, EXPIRED), points, rupee value equivalent, and order reference.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDTO {
    private UUID transactionId;
    private String type;
    private BigDecimal points;
    private MoneyDTO rupeeValue;
    private UUID orderId;
    private String description;
    private OffsetDateTime createdAt;
}
