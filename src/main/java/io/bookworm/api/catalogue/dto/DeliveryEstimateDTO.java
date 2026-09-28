package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Delivery estimate details for a product/format.
 * <p>
 * Why: Conveys estimated delivery date, business days duration, and shipping cost.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEstimateDTO {
    private LocalDate estimatedDeliveryDate;
    private Integer estimatedDays;
    private MoneyDTO shippingCost;
    private Boolean isFreeDelivery;
}
