package io.bookworm.api.checkout.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.user.dto.AddressDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representation of an active checkout session state.
 * <p>
 * Why: Transports session ID, lifecycle status (CREATED/ADDRESS_SET/COUPON_APPLIED/WALLET_APPLIED/PAYMENT_INITIATED/CONFIRMED/EXPIRED),
 * address snapshot, and expiration timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionResponse {
    private UUID sessionId;
    private String status;
    private AddressDTO deliveryAddress;
    private CouponSummaryDTO appliedCoupon;
    private MoneyDTO walletPointsRedeemed;
    private PriceSummaryDTO priceSummary;
    private OffsetDateTime expiresAt;
}
