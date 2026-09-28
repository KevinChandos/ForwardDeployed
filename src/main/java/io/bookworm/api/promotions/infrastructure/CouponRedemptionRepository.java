package io.bookworm.api.promotions.infrastructure;

import io.bookworm.api.promotions.domain.CouponRedemption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link CouponRedemption} entities.
 * <p>
 * Why: tracks individual redemptions of coupons against orders, enabling per-user coupon usage checks.
 */
@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID>, JpaSpecificationExecutor<CouponRedemption> {

    /**
     * Finds the coupon redemption record for a specific order.
     */
    @Query("""
           SELECT r FROM CouponRedemption r
           WHERE r.order.orderId = :orderId
             AND r.deletedAt IS NULL
           """)
    Optional<CouponRedemption> findActiveByOrderId(@Param("orderId") UUID orderId);

    /**
     * Counts how many times a member has redeemed a specific coupon.
     */
    @Query("""
           SELECT COUNT(r) FROM CouponRedemption r
           WHERE r.coupon.couponId = :couponId
             AND r.member.memberId = :memberId
             AND r.deletedAt IS NULL
           """)
    long countActiveRedemptionsByMember(
            @Param("couponId") UUID couponId,
            @Param("memberId") UUID memberId);

    /**
     * Counts how many times a guest token has redeemed a specific coupon.
     */
    @Query("""
           SELECT COUNT(r) FROM CouponRedemption r
           WHERE r.coupon.couponId = :couponId
             AND r.guestToken = :guestToken
             AND r.deletedAt IS NULL
           """)
    long countActiveRedemptionsByGuest(
            @Param("couponId") UUID couponId,
            @Param("guestToken") String guestToken);

    /**
     * Pages all redemptions for a specific coupon.
     */
    @Query(value = """
           SELECT r FROM CouponRedemption r
           WHERE r.coupon.couponId = :couponId
             AND r.deletedAt IS NULL
           ORDER BY r.redeemedAt DESC
           """,
           countQuery = """
           SELECT COUNT(r) FROM CouponRedemption r
           WHERE r.coupon.couponId = :couponId
             AND r.deletedAt IS NULL
           """)
    Page<CouponRedemption> findActiveByCouponId(@Param("couponId") UUID couponId, Pageable pageable);
}
