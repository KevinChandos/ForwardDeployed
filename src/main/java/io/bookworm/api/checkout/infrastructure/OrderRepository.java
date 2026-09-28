package io.bookworm.api.checkout.infrastructure;

import io.bookworm.api.checkout.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Order} entities.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Finds an active order by ID (excludes soft-deleted rows). */
    @Query("SELECT o FROM Order o WHERE o.orderId = :id AND o.deletedAt IS NULL")
    Optional<Order> findActive(@Param("id") UUID id);

    /**
     * Finds an active order belonging to a specific member.
     * Used to verify ownership before status transitions.
     */
    @Query("""
           SELECT o FROM Order o
           WHERE o.orderId = :orderId
             AND o.member.memberId = :memberId
             AND o.deletedAt IS NULL
           """)
    Optional<Order> findActiveByIdAndMemberId(
            @Param("orderId") UUID orderId,
            @Param("memberId") UUID memberId);

    /**
     * Pages through all active orders for a member, ordered by most recent first.
     */
    @Query(value = """
           SELECT o FROM Order o
           WHERE o.member.memberId = :memberId
             AND o.deletedAt IS NULL
           ORDER BY o.placedAt DESC
           """,
           countQuery = """
           SELECT COUNT(o) FROM Order o
           WHERE o.member.memberId = :memberId
             AND o.deletedAt IS NULL
           """)
    Page<Order> findActiveByMemberId(
            @Param("memberId") UUID memberId,
            Pageable pageable);
}
