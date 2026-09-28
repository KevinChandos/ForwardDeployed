package io.bookworm.api.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Return shipment status response DTO.
 * <p>
 * Why: Transports courier pickup and reverse logistics timeline for processed return requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnShipmentResponse {
    private UUID returnShipmentId;
    private UUID returnRequestId;
    private String carrier;
    private String trackingNumber;
    private String status;
    private OffsetDateTime pickedUpAt;
    private OffsetDateTime receivedAt;
}
