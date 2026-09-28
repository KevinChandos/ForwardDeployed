package io.bookworm.api.checkout.mapper;

import io.bookworm.api.catalogue.mapper.CatalogueMapper;
import io.bookworm.api.checkout.domain.*;
import io.bookworm.api.checkout.dto.*;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.user.dto.AddressDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for Checkout & Ordering bounded context.
 * <p>
 * Why: Converts Order, OrderLine, CheckoutSession, and ReturnRequest entities into client DTOs with full null safety.
 */
@Mapper(
    componentModel = "spring",
    uses = {CatalogueMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CheckoutMapper {

    // ── OrderLine Mappings ───────────────────────────────────────────────────
    @Mapping(target = "orderLineId", source = "line.orderLineId")
    @Mapping(target = "bookId", source = "line.book.bookId")
    @Mapping(target = "title", source = "line.titleSnapshot")
    @Mapping(target = "author", source = "line.authorSnapshot")
    @Mapping(target = "formatType", source = "line.formatSnapshot")
    @Mapping(target = "quantity", source = "line.quantity")
    @Mapping(target = "unitPrice", source = "line.unitPrice", qualifiedByName = "amountToMoneyDTO")
    @Mapping(target = "subtotal", source = "line.subtotal", qualifiedByName = "amountToMoneyDTO")
    OrderLineDTO toOrderLineDTO(OrderLine line);

    List<OrderLineDTO> toOrderLineDTOList(List<OrderLine> lines);

    // ── Address Mappings ─────────────────────────────────────────────────────
    AddressDTO toAddressDTO(OrderDeliveryAddress address);

    AddressDTO toAddressDTO(CheckoutAddress address);

    @Mapping(target = "deliveryAddressId", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    OrderDeliveryAddress toOrderDeliveryAddress(AddressDTO addressDTO);

    // ── ReturnRequest Mappings ───────────────────────────────────────────────
    @Mapping(target = "returnRequestId", source = "returnRequest.returnRequestId")
    @Mapping(target = "status", source = "returnRequest.status")
    @Mapping(target = "reason", source = "returnRequest.reason")
    @Mapping(target = "refundAmount", source = "returnRequest.refundAmount", qualifiedByName = "amountToMoneyDTO")
    @Mapping(target = "requestedAt", source = "returnRequest.requestedAt")
    ReturnRequestDTO toReturnRequestDTO(ReturnRequest returnRequest);

    // ── Order Mappings ───────────────────────────────────────────────────────
    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "placedAt", source = "order.placedAt")
    @Mapping(target = "itemCount", source = "itemCount")
    @Mapping(target = "totalPayable", source = "order.totalPayable", qualifiedByName = "amountToMoneyDTO")
    @Mapping(target = "paymentStatus", source = "paymentStatus")
    @Mapping(target = "shipmentStatus", source = "shipmentStatus")
    OrderSummaryDTO toOrderSummaryDTO(Order order, Integer itemCount, String paymentStatus, String shipmentStatus);

    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "placedAt", source = "order.placedAt")
    @Mapping(target = "lines", source = "lines")
    @Mapping(target = "deliveryAddress", source = "deliveryAddress")
    @Mapping(target = "priceSummary", source = "priceSummary")
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "shipment", source = "shipment")
    @Mapping(target = "returnRequest", source = "returnRequest")
    @Mapping(target = "version", source = "order.version")
    OrderDetailResponse toOrderDetailResponse(
            Order order,
            List<OrderLineDTO> lines,
            AddressDTO deliveryAddress,
            PriceSummaryDTO priceSummary,
            PaymentSummaryDTO payment,
            ShipmentSummaryDTO shipment,
            ReturnRequestDTO returnRequest);

    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "placedAt", source = "order.placedAt")
    @Mapping(target = "lines", source = "lines")
    @Mapping(target = "deliveryAddress", source = "deliveryAddress")
    @Mapping(target = "priceSummary", source = "priceSummary")
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "shipment", source = "shipment")
    OrderConfirmationResponse toOrderConfirmationResponse(
            Order order,
            List<OrderLineDTO> lines,
            AddressDTO deliveryAddress,
            PriceSummaryDTO priceSummary,
            PaymentSummaryDTO payment,
            ShipmentSummaryDTO shipment);

    // ── Helper Converters ────────────────────────────────────────────────────
    @Named("amountToMoneyDTO")
    default MoneyDTO amountToMoneyDTO(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return MoneyDTO.builder()
                .amount(amount.toPlainString())
                .currency("INR")
                .build();
    }
}
