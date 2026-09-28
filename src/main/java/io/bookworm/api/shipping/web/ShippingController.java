package io.bookworm.api.shipping.web;

import io.bookworm.api.shipping.application.ShippingService;
import io.bookworm.api.shipping.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for shipping and delivery: pre-purchase delivery estimates,
 * order shipment tracking, and return shipment tracking.
 * <p>
 * Why: Separates the shipping bounded context HTTP layer from both the catalogue
 * (delivery estimate at browse time) and order contexts (post-dispatch tracking),
 * letting the ShippingService own all carrier-integration logic.
 */
@Tag(name = "Shipping", description = "Delivery estimates and shipment tracking")
@RestController
@Validated
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    // ── GET /shipping/estimate ────────────────────────────────────────────────

    @Operation(
            operationId = "getDeliveryEstimate",
            summary = "Get delivery estimate for a book format and pin code"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery estimate"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Book format not found"),
            @ApiResponse(responseCode = "422", description = "Pin code not serviceable")
    })
    @GetMapping("/shipping/estimate")
    public ResponseEntity<DeliveryEstimateResponse> getDeliveryEstimate(
            @Parameter(description = "Book format UUID", required = true)
            @RequestParam @NotNull UUID bookFormatId,
            @Parameter(description = "Delivery pin code (3–20 chars)", required = true)
            @RequestParam @Size(min = 3, max = 20) String pinCode) {

        DeliveryEstimateResponse response = shippingService.getDeliveryEstimate(pinCode, null);
        return ResponseEntity.ok(response);
    }

    // ── GET /orders/{orderId}/shipment ────────────────────────────────────────

    @Operation(
            operationId = "getShipmentTracking",
            summary = "Get shipment tracking for an order",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment tracking info"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Shipment not found for order")
    })
    @GetMapping("/orders/{orderId}/shipment")
    public ResponseEntity<ShipmentTrackingResponse> getShipmentTracking(
            @PathVariable UUID orderId) {

        ShipmentTrackingResponse response = shippingService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    // ── GET /orders/{orderId}/return/shipment ─────────────────────────────────

    @Operation(
            operationId = "getReturnShipmentTracking",
            summary = "Get return shipment tracking",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return shipment tracking"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Return shipment not found")
    })
    @GetMapping("/orders/{orderId}/return/shipment")
    public ResponseEntity<ReturnShipmentResponse> getReturnShipmentTracking(
            @PathVariable UUID orderId) {

        ReturnShipmentResponse response = shippingService.getReturnShipment(orderId);
        return ResponseEntity.ok(response);
    }
}
