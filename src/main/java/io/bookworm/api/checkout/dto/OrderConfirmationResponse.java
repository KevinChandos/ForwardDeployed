package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.user.dto.AddressDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order confirmation response DTO returned upon successful placement.
 * <p>
 * Why: Transports order confirmation number, items, immutable address snapshot, payment summary, and tracking estimate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmationResponse {
    private UUID orderId;
    private String orderNumber;
    private String status;
    private OffsetDateTime placedAt;
    private List<OrderLineDTO> lines;
    private AddressDTO deliveryAddress;
    private PriceSummaryDTO priceSummary;
    private PaymentSummaryDTO payment;
    private ShipmentSummaryDTO shipment;
}
