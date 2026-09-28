package io.bookworm.api.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Individual tracking checkpoint event within a shipment timeline.
 * <p>
 * Why: Transports carrier event type, location, courier notes, and event occurrence timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentEventDTO {
    private String eventType;
    private String location;
    private String notes;
    private OffsetDateTime occurredAt;
}
