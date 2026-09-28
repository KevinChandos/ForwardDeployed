package io.bookworm.api.shipping.mapper;

import io.bookworm.api.shipping.domain.ReturnShipment;
import io.bookworm.api.shipping.domain.Shipment;
import io.bookworm.api.shipping.domain.ShipmentEvent;
import io.bookworm.api.shipping.dto.ReturnShipmentResponse;
import io.bookworm.api.shipping.dto.ShipmentEventDTO;
import io.bookworm.api.shipping.dto.ShipmentTrackingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Shipping & Fulfilment bounded context.
 * <p>
 * Why: Converts Shipment, ShipmentEvent, and ReturnShipment entities into tracking DTOs with null safety.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ShippingMapper {

    @Mapping(target = "eventType", source = "event.eventType")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "notes", source = "event.notes")
    @Mapping(target = "occurredAt", source = "event.occurredAt")
    ShipmentEventDTO toShipmentEventDTO(ShipmentEvent event);

    List<ShipmentEventDTO> toShipmentEventDTOList(List<ShipmentEvent> events);

    @Mapping(target = "shipmentId", source = "shipment.shipmentId")
    @Mapping(target = "orderId", source = "shipment.order.orderId")
    @Mapping(target = "status", source = "shipment.status")
    @Mapping(target = "carrier", source = "shipment.carrier")
    @Mapping(target = "trackingNumber", source = "shipment.trackingNumber")
    @Mapping(target = "estimatedDelivery", source = "shipment.estimatedDeliveryDate")
    @Mapping(target = "dispatchedAt", source = "shipment.dispatchedAt")
    @Mapping(target = "deliveredAt", source = "shipment.deliveredAt")
    @Mapping(target = "events", source = "events")
    ShipmentTrackingResponse toShipmentTrackingResponse(Shipment shipment, List<ShipmentEventDTO> events);

    @Mapping(target = "returnShipmentId", source = "returnShipment.returnShipmentId")
    @Mapping(target = "returnRequestId", source = "returnShipment.returnRequest.returnRequestId")
    @Mapping(target = "carrier", source = "returnShipment.carrier")
    @Mapping(target = "trackingNumber", source = "returnShipment.trackingNumber")
    @Mapping(target = "status", source = "returnShipment.status")
    @Mapping(target = "pickedUpAt", source = "returnShipment.pickedUpAt")
    @Mapping(target = "receivedAt", source = "returnShipment.receivedAt")
    ReturnShipmentResponse toReturnShipmentResponse(ReturnShipment returnShipment);
}
