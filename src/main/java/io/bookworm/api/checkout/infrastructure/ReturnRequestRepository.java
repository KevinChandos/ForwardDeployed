package io.bookworm.api.checkout.infrastructure;

import io.bookworm.api.checkout.domain.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link ReturnRequest} entities.
 */
@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    /** Returns all active return requests for a specific order. */
    @Query("""
           SELECT r FROM ReturnRequest r
           WHERE r.order.orderId = :orderId
             AND r.deletedAt IS NULL
           ORDER BY r.requestedAt DESC
           """)
    List<ReturnRequest> findActiveByOrderId(@Param("orderId") UUID orderId);
}
