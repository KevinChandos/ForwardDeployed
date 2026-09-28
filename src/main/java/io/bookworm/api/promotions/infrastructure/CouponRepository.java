package io.bookworm.api.promotions.infrastructure;

import io.bookworm.api.promotions.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Coupon} entities.
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    /**
     * Finds an active coupon by its uppercase code within a store.
     * Used during coupon validation at checkout.
     */
    @Query("""
           SELECT c FROM Coupon c
           WHERE c.store.storeId = :storeId
             AND c.code = :code
             AND c.isActive = true
             AND c.deletedAt IS NULL
             AND (c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)
           """)
    Optional<Coupon> findActiveByStoreAndCode(
            @Param("storeId") UUID storeId,
            @Param("code") String code);

    /** Pages through all coupons (including inactive) for a store — admin view. */
    @Query(value = "SELECT c FROM Coupon c WHERE c.store.storeId = :storeId AND c.deletedAt IS NULL",
           countQuery = "SELECT COUNT(c) FROM Coupon c WHERE c.store.storeId = :storeId AND c.deletedAt IS NULL")
    Page<Coupon> findAllActiveByStoreId(
            @Param("storeId") UUID storeId,
            Pageable pageable);
}
