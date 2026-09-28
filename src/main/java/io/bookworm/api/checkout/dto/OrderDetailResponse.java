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
 * Detailed order response representation.
 * <p>
 * Why: Encapsulates all order lines, historical price breakdown, address snapshot, shipment tracker, and return request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private UUID orderId;
    private String orderNumber;
    private String status;
    private OffsetDateTime placedAt;
    private List<OrderLineDTO> lines;
    private AddressDTO deliveryAddress;
    private PriceSummaryDTO priceSummary;
    private PaymentSummaryDTO payment;
    private ShipmentSummaryDTO shipment;
    private ReturnRequestDTO returnRequest;
    private Integer version;
}
