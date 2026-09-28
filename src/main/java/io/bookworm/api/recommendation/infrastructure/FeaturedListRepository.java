package io.bookworm.api.recommendation.infrastructure;

import io.bookworm.api.recommendation.domain.FeaturedList;
import io.bookworm.api.recommendation.domain.FeaturedList.ListType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link FeaturedList} entities.
 * <p>
 * Why: retrieves active featured lists (bestsellers, new launches, etc.) for a store's storefront.
 */
@Repository
public interface FeaturedListRepository extends JpaRepository<FeaturedList, UUID>, JpaSpecificationExecutor<FeaturedList> {

    /**
     * Finds the currently active featured list for a specific store and list type, with entries eagerly loaded.
     */
    @Query("""
           SELECT DISTINCT l FROM FeaturedList l
           LEFT JOIN FETCH l.entries e
           LEFT JOIN FETCH e.book b
           WHERE l.store.storeId = :storeId
             AND l.listType = :listType
             AND l.effectiveFrom <= :now
             AND (l.effectiveTo IS NULL OR l.effectiveTo > :now)
             AND l.deletedAt IS NULL
           """)
    Optional<FeaturedList> findCurrentWithEntries(
            @Param("storeId") UUID storeId,
            @Param("listType") ListType listType,
            @Param("now") OffsetDateTime now);

    /**
     * Pages all featured lists for a specific store.
     */
    @Query(value = """
           SELECT l FROM FeaturedList l
           WHERE l.store.storeId = :storeId
             AND l.deletedAt IS NULL
           ORDER BY l.effectiveFrom DESC
           """,
           countQuery = """
           SELECT COUNT(l) FROM FeaturedList l
           WHERE l.store.storeId = :storeId
             AND l.deletedAt IS NULL
           """)
    Page<FeaturedList> findAllByStoreId(@Param("storeId") UUID storeId, Pageable pageable);
}
