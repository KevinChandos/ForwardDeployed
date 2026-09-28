package io.bookworm.api.store.infrastructure;

import io.bookworm.api.store.domain.TaxRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link TaxRule} entities.
 * <p>
 * Why: supports time-bounded tax rule lookups for order calculations and store administration.
 */
@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, UUID>, JpaSpecificationExecutor<TaxRule> {

    /**
     * Finds the currently effective tax rule for a given category in a store.
     * Active rule has effectiveFrom <= date and (effectiveTo IS NULL OR effectiveTo > date).
     */
    @Query("""
           SELECT t FROM TaxRule t
           WHERE t.store.storeId = :storeId
             AND t.taxCategory = :taxCategory
             AND t.effectiveFrom <= :date
             AND (t.effectiveTo IS NULL OR t.effectiveTo > :date)
             AND t.deletedAt IS NULL
           """)
    Optional<TaxRule> findCurrentRule(
            @Param("storeId") UUID storeId,
            @Param("taxCategory") String taxCategory,
            @Param("date") LocalDate date);

    /**
     * Finds all currently effective tax rules for a store.
     */
    @Query("""
           SELECT t FROM TaxRule t
           WHERE t.store.storeId = :storeId
             AND t.effectiveFrom <= :date
             AND (t.effectiveTo IS NULL OR t.effectiveTo > :date)
             AND t.deletedAt IS NULL
           """)
    List<TaxRule> findAllEffectiveByStoreId(
            @Param("storeId") UUID storeId,
            @Param("date") LocalDate date);

    /**
     * Pages all tax rules for a store (historical and current).
     */
    @Query(value = """
           SELECT t FROM TaxRule t
           WHERE t.store.storeId = :storeId
             AND t.deletedAt IS NULL
           ORDER BY t.taxCategory ASC, t.effectiveFrom DESC
           """,
           countQuery = """
           SELECT COUNT(t) FROM TaxRule t
           WHERE t.store.storeId = :storeId
             AND t.deletedAt IS NULL
           """)
    Page<TaxRule> findAllByStoreId(@Param("storeId") UUID storeId, Pageable pageable);
}
