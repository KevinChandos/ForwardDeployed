package io.bookworm.api.store.infrastructure;

import io.bookworm.api.store.domain.DeliveryThreshold;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link DeliveryThreshold} entities.
 * <p>
 * Why: supports calculation of tiered shipping costs per store based on order subtotal.
 * Includes specifications for dynamic administration filtering and pagination.
 */
@Repository
public interface DeliveryThresholdRepository extends JpaRepository<DeliveryThreshold, UUID>, JpaSpecificationExecutor<DeliveryThreshold> {

    /**
     * Finds all active delivery thresholds for a specific store, ordered by min order amount ascending.
     */
    @Query("""
           SELECT d FROM DeliveryThreshold d
           WHERE d.store.storeId = :storeId
             AND d.deletedAt IS NULL
           ORDER BY d.minOrderAmount ASC
           """)
    List<DeliveryThreshold> findActiveByStoreId(@Param("storeId") UUID storeId);

    /**
     * Finds all active delivery thresholds for a specific store with pagination.
     */
    @Query(value = """
           SELECT d FROM DeliveryThreshold d
           WHERE d.store.storeId = :storeId
             AND d.deletedAt IS NULL
           ORDER BY d.minOrderAmount ASC
           """,
           countQuery = """
           SELECT COUNT(d) FROM DeliveryThreshold d
           WHERE d.store.storeId = :storeId
             AND d.deletedAt IS NULL
           """)
    Page<DeliveryThreshold> findActiveByStoreId(@Param("storeId") UUID storeId, Pageable pageable);

    /**
     * Finds the applicable delivery threshold tier for an order subtotal.
     * Selects the tier with highest minOrderAmount <= orderAmount.
     */
    @Query("""
           SELECT d FROM DeliveryThreshold d
           WHERE d.store.storeId = :storeId
             AND d.minOrderAmount <= :orderAmount
             AND d.deletedAt IS NULL
           ORDER BY d.minOrderAmount DESC
           LIMIT 1
           """)
    Optional<DeliveryThreshold> findApplicableThreshold(
            @Param("storeId") UUID storeId,
            @Param("orderAmount") BigDecimal orderAmount);
}
