package io.bookworm.api.common.exception;

/**
 * Thrown when a coupon or promotional code cannot be applied because it is expired,
 * already redeemed, or exceeds its usage limit.
 * <p>
 * Why: Subclasses {@link BusinessRuleException} so the global handler returns HTTP 422
 * with a dedicated {@code COUPON_INVALID} code, allowing clients to surface a specific
 * redemption-failure message rather than a generic error.
 * <p>
 * Side effects: Triggers transaction rollback; no order mutations are persisted.
 */
public class CouponExpiredException extends BusinessRuleException {

    private final String couponCode;

    public CouponExpiredException(String couponCode) {
        super("COUPON_INVALID",
                String.format("Coupon '%s' is expired, already used, or not applicable to this order",
                        couponCode));
        this.couponCode = couponCode;
    }

    public CouponExpiredException(String couponCode, String detail) {
        super("COUPON_INVALID", detail);
        this.couponCode = couponCode;
    }

    public String getCouponCode() {
        return couponCode;
    }
}
