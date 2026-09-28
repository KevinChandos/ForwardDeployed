package io.bookworm.api.promotions.application;

import io.bookworm.api.promotions.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for promotional coupons management and checkout validation.
 * <p>
 * Why: Encapsulates coupon creation, lifecycle status updates, redemption limit enforcement,
 * and pre-checkout validation calculations.
 */
public interface CouponService {

    /**
     * Validates coupon applicability and computes projected discount savings for a cart total.
     */
    CouponValidationResponse validateCoupon(UUID storeId, ValidateCouponRequest request);

    /**
     * Pages all coupons for administrative management in a store.
     */
    CouponListResponse getCoupons(UUID storeId, Pageable pageable);

    /**
     * Creates a new promotional discount coupon for a store.
     */
    CouponDTO createCoupon(UUID storeId, CreateCouponRequest request);

    /**
     * Updates an existing coupon's properties.
     */
    CouponDTO updateCoupon(UUID couponId, UpdateCouponRequest request);

    /**
     * Activates or deactivates a coupon.
     */
    void updateCouponStatus(UUID couponId, CouponUpdateStatusRequest request);

    /**
     * Deletes a coupon.
     */
    void deleteCoupon(UUID couponId);
}
