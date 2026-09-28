package io.bookworm.api.checkout.web;

import io.bookworm.api.checkout.application.OrderService;
import io.bookworm.api.checkout.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for placed order management: history listing, detail retrieval,
 * cancellation, and return request submission.
 * <p>
 * Why: Manages the post-confirmation order lifecycle independently of the checkout
 * flow controller — orders are immutable after placement except for cancellation
 * (pre-dispatch) and return requests (post-delivery).
 */
@Tag(name = "Orders", description = "Checkout flow and order lifecycle")
@RestController
@RequestMapping("/orders")
@Validated
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── GET /orders ───────────────────────────────────────────────────────────

    @Operation(operationId = "listMyOrders", summary = "List member order history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated order list"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @GetMapping
    public ResponseEntity<OrderListResponse> listMyOrders(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String status,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {

        UUID memberId = resolveId(principal);
        Sort sortSpec = sort != null ? parseSort(sort) : Sort.by(Sort.Direction.DESC, "placedAt");
        PageRequest pageable = PageRequest.of(page - 1, size, sortSpec);
        OrderListResponse response = orderService.getMemberOrders(memberId, pageable);
        return ResponseEntity.ok(response);
    }

    // ── GET /orders/{orderId} ─────────────────────────────────────────────────

    @Operation(operationId = "getOrder", summary = "Get full order detail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order detail"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrder(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID orderId) {

        UUID memberId = resolveId(principal);
        OrderDetailResponse response = orderService.getOrderById(orderId, memberId);
        return ResponseEntity.ok(response);
    }

    // ── POST /orders/{orderId}/cancel ─────────────────────────────────────────

    @Operation(operationId = "cancelOrder", summary = "Cancel an order before dispatch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict"),
            @ApiResponse(responseCode = "422", description = "Order already dispatched or delivered")
    })
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request) {

        UUID memberId = resolveId(principal);
        orderService.cancelOrder(orderId, memberId, request);
        OrderDetailResponse response = orderService.getOrderById(orderId, memberId);
        return ResponseEntity.ok(response);
    }

    // ── POST /orders/{orderId}/return ─────────────────────────────────────────

    @Operation(operationId = "requestReturn", summary = "Request a return for a delivered order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Return request created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Return already requested"),
            @ApiResponse(responseCode = "422", description = "Order not in returnable state")
    })
    @PostMapping("/{orderId}/return")
    public ResponseEntity<ReturnRequestDTO> requestReturn(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody ReturnRequestBody request) {

        UUID memberId = resolveId(principal);
        ReturnRequestDTO dto = orderService.requestReturn(orderId, memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UUID resolveId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(":");
        if (parts.length == 2) {
            Sort.Direction dir = "desc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, parts[0]);
        }
        return Sort.by(parts[0]);
    }
}
