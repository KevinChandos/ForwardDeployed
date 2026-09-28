package io.bookworm.api.promotions.infrastructure;

import io.bookworm.api.promotions.domain.GiftPointPolicy;
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
 * JPA repository for {@link GiftPointPolicy} entities.
 * <p>
 * Why: retrieves store-specific gift point policies for point accrual and redemption limits.
 */
@Repository
public interface GiftPointPolicyRepository extends JpaRepository<GiftPointPolicy, UUID>, JpaSpecificationExecutor<GiftPointPolicy> {

    /**
     * Finds the currently active gift points policy for a store.
     */
    @Query("""
           SELECT p FROM GiftPointPolicy p
           WHERE p.store.storeId = :storeId
             AND p.isActive = true
             AND p.deletedAt IS NULL
           """)
    Optional<GiftPointPolicy> findActiveByStoreId(@Param("storeId") UUID storeId);

    /**
     * Pages all gift point policies for a store (policy history).
     */
    @Query(value = """
           SELECT p FROM GiftPointPolicy p
           WHERE p.store.storeId = :storeId
             AND p.deletedAt IS NULL
           ORDER BY p.createdAt DESC
           """,
           countQuery = """
           SELECT COUNT(p) FROM GiftPointPolicy p
           WHERE p.store.storeId = :storeId
             AND p.deletedAt IS NULL
           """)
    Page<GiftPointPolicy> findAllByStoreId(@Param("storeId") UUID storeId, Pageable pageable);
}
