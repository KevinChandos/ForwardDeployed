package io.bookworm.api.shipping.application;

import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.shipping.domain.ReturnShipment;
import io.bookworm.api.shipping.domain.Shipment;
import io.bookworm.api.shipping.dto.DeliveryEstimateResponse;
import io.bookworm.api.shipping.dto.ReturnShipmentResponse;
import io.bookworm.api.shipping.dto.ShipmentEventDTO;
import io.bookworm.api.shipping.dto.ShipmentTrackingResponse;
import io.bookworm.api.shipping.infrastructure.ReturnShipmentRepository;
import io.bookworm.api.shipping.infrastructure.ShipmentRepository;
import io.bookworm.api.shipping.mapper.ShippingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Shipping & Fulfilment bounded context.
 * <p>
 * Why: Orchestrates delivery timeframe estimation, live shipment tracking timelines,
 * and reverse logistics return shipments.
 * Side effects: Queries Shipment, ShipmentEvent, and ReturnShipment records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingServiceImpl implements ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final ReturnShipmentRepository returnShipmentRepository;
    private final ShippingMapper shippingMapper;

    @Override
    public DeliveryEstimateResponse getDeliveryEstimate(String pinCode, UUID storeId) {
        log.info("Calculating delivery estimate for pinCode: {}, storeId: {}", pinCode, storeId);

        if (pinCode == null || pinCode.trim().length() < 6) {
            throw new BusinessRuleException("INVALID_PINCODE", "A valid 6-digit pin code is required");
        }

        // Standard 3-5 business days delivery algorithm
        int estimatedDays = pinCode.startsWith("11") || pinCode.startsWith("40") || pinCode.startsWith("56") ? 2 : 4;
        LocalDate estimatedDate = LocalDate.now().plusDays(estimatedDays);

        return DeliveryEstimateResponse.builder()
                .pinCode(pinCode.trim())
                .estimatedDeliveryDate(estimatedDate)
                .estimatedDays(estimatedDays)
                .shippingCost(MoneyDTO.builder().amount("50.00").currency("INR").build())
                .isFreeDelivery(false)
                .build();
    }

    @Override
    public ShipmentTrackingResponse getShipmentTracking(UUID shipmentId) {
        log.info("Fetching shipment tracking for shipmentId: {}", shipmentId);
        Shipment shipment = shipmentRepository.findActiveWithEventsById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        List<ShipmentEventDTO> events = shipment.getEvents() != null ?
                shippingMapper.toShipmentEventDTOList(shipment.getEvents()) : Collections.emptyList();

        return shippingMapper.toShipmentTrackingResponse(shipment, events);
    }

    @Override
    public ShipmentTrackingResponse getShipmentByOrderId(UUID orderId) {
        log.info("Fetching shipment tracking for orderId: {}", orderId);
        Shipment shipment = shipmentRepository.findActiveByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment for order", orderId));

        return getShipmentTracking(shipment.getShipmentId());
    }

    @Override
    public ReturnShipmentResponse getReturnShipment(UUID returnRequestId) {
        log.info("Fetching return shipment for returnRequestId: {}", returnRequestId);
        ReturnShipment returnShipment = returnShipmentRepository.findActiveByReturnRequestId(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnShipment for request", returnRequestId));

        return shippingMapper.toReturnShipmentResponse(returnShipment);
    }
}
