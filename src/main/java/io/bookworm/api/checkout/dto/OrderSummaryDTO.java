package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary representation of an order for order history listings.
 * <p>
 * Why: Transports order ID, reference number, total items, payment status, delivery status, and grand total.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    private UUID orderId;
    private String orderNumber;
    private String status;
    private OffsetDateTime placedAt;
    private Integer itemCount;
    private MoneyDTO totalPayable;
    private String paymentStatus;
    private String shipmentStatus;
}
