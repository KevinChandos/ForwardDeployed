package io.bookworm.api.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detailed shipment tracking timeline response.
 * <p>
 * Why: Encapsulates shipment status, carrier, tracking number, estimated delivery, and ordered tracking events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTrackingResponse {
    private UUID shipmentId;
    private UUID orderId;
    private String status;
    private String carrier;
    private String trackingNumber;
    private LocalDate estimatedDelivery;
    private OffsetDateTime dispatchedAt;
    private OffsetDateTime deliveredAt;
    private List<ShipmentEventDTO> events;
}
