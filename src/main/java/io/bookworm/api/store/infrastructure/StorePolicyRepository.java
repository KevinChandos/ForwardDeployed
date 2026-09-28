package io.bookworm.api.store.infrastructure;

import io.bookworm.api.store.domain.StorePolicy;
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
 * JPA repository for {@link StorePolicy} entities.
 * <p>
 * Why: queries active policies per store to govern return windows and free delivery thresholds.
 */
@Repository
public interface StorePolicyRepository extends JpaRepository<StorePolicy, UUID>, JpaSpecificationExecutor<StorePolicy> {

    /**
     * Finds the currently active policy governing a specific store.
     */
    @Query("""
           SELECT p FROM StorePolicy p
           WHERE p.store.storeId = :storeId
             AND p.isActive = true
             AND p.deletedAt IS NULL
           """)
    Optional<StorePolicy> findActiveByStoreId(@Param("storeId") UUID storeId);

    /**
     * Finds all policies for a store with pagination (policy history view).
     */
    @Query(value = """
           SELECT p FROM StorePolicy p
           WHERE p.store.storeId = :storeId
             AND p.deletedAt IS NULL
           ORDER BY p.createdAt DESC
           """,
           countQuery = """
           SELECT COUNT(p) FROM StorePolicy p
           WHERE p.store.storeId = :storeId
             AND p.deletedAt IS NULL
           """)
    Page<StorePolicy> findAllByStoreId(@Param("storeId") UUID storeId, Pageable pageable);
}
