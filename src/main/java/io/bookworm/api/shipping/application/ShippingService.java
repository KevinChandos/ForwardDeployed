package io.bookworm.api.shipping.application;

import io.bookworm.api.shipping.dto.DeliveryEstimateResponse;
import io.bookworm.api.shipping.dto.ReturnShipmentResponse;
import io.bookworm.api.shipping.dto.ShipmentTrackingResponse;

import java.util.UUID;

/**
 * Service interface for shipping, carrier delivery estimates, tracking timeline, and return shipments.
 * <p>
 * Why: Encapsulates delivery timeline calculation, courier tracking event lookup, and reverse logistics dispatch.
 */
public interface ShippingService {

    /**
     * Calculates delivery estimate and shipping fee for a target pin code.
     */
    DeliveryEstimateResponse getDeliveryEstimate(String pinCode, UUID storeId);

    /**
     * Retrieves the tracking timeline and current status of an order shipment.
     */
    ShipmentTrackingResponse getShipmentTracking(UUID shipmentId);

    /**
     * Retrieves shipment tracking by order ID.
     */
    ShipmentTrackingResponse getShipmentByOrderId(UUID orderId);

    /**
     * Retrieves return shipment reverse logistics details for a return request.
     */
    ReturnShipmentResponse getReturnShipment(UUID returnRequestId);
}
