package io.bookworm.api.shipping.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response payload for shipping delivery estimate calculations.
 * <p>
 * Why: Conveys estimated delivery date, business days, calculated shipping cost, and free delivery qualification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEstimateResponse {
    private String pinCode;
    private LocalDate estimatedDeliveryDate;
    private Integer estimatedDays;
    private MoneyDTO shippingCost;
    private Boolean isFreeDelivery;
}
