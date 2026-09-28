package io.bookworm.api.checkout.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shipment summary DTO for order views.
 * <p>
 * Why: Transports shipment tracking ID, courier name, tracking number, status, and estimated delivery date.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentSummaryDTO {
    private UUID shipmentId;
    private String status;
    private String trackingNumber;
    private String carrier;
    private LocalDate estimatedDelivery;
}
