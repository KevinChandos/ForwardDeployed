package io.bookworm.api.store.infrastructure;

import io.bookworm.api.store.domain.Store;
import io.bookworm.api.store.domain.Store.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Store} entities.
 * <p>
 * Why: {@link JpaSpecificationExecutor} is supported for dynamic store filtering
 * (e.g., status, region, owner). Custom queries enforce active / non-deleted filtering.
 * Side effects: soft-deleted stores are filtered out using {@code deletedAt IS NULL}.
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, UUID>, JpaSpecificationExecutor<Store> {

    /**
     * Finds an active store by its primary key ID excluding soft-deleted rows.
     */
    @Query("SELECT s FROM Store s WHERE s.storeId = :storeId AND s.deletedAt IS NULL")
    Optional<Store> findActiveById(@Param("storeId") UUID storeId);

    /**
     * Finds an active store by its URL slug.
     * Used in public storefront routing.
     */
    @Query("SELECT s FROM Store s WHERE s.slug = :slug AND s.deletedAt IS NULL")
    Optional<Store> findActiveBySlug(@Param("slug") String slug);

    /**
     * Finds an active store by slug and status.
     */
    @Query("SELECT s FROM Store s WHERE s.slug = :slug AND s.status = :status AND s.deletedAt IS NULL")
    Optional<Store> findBySlugAndStatus(@Param("slug") String slug, @Param("status") StoreStatus status);

    /**
     * Checks whether an active store with the given slug already exists.
     */
    @Query("SELECT COUNT(s) > 0 FROM Store s WHERE s.slug = :slug AND s.deletedAt IS NULL")
    boolean existsActiveBySlug(@Param("slug") String slug);

    /**
     * Pages all stores by status excluding soft-deleted rows.
     */
    @Query(value = "SELECT s FROM Store s WHERE s.status = :status AND s.deletedAt IS NULL",
           countQuery = "SELECT COUNT(s) FROM Store s WHERE s.status = :status AND s.deletedAt IS NULL")
    Page<Store> findAllByStatus(@Param("status") StoreStatus status, Pageable pageable);

    /**
     * Returns all active stores owned by a specific member.
     */
    @Query("SELECT s FROM Store s WHERE s.ownerMember.memberId = :ownerMemberId AND s.deletedAt IS NULL")
    List<Store> findByOwnerMemberId(@Param("ownerMemberId") UUID ownerMemberId);
}
