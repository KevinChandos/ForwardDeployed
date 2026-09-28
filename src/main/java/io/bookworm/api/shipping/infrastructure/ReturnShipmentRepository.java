package io.bookworm.api.shipping.infrastructure;

import io.bookworm.api.shipping.domain.ReturnShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link ReturnShipment} entities.
 */
@Repository
public interface ReturnShipmentRepository extends JpaRepository<ReturnShipment, UUID> {

    /**
     * Finds the active return shipment for a specific return request.
     */
    @Query("""
           SELECT rs FROM ReturnShipment rs
           WHERE rs.returnRequest.returnRequestId = :returnRequestId
             AND rs.deletedAt IS NULL
           """)
    Optional<ReturnShipment> findActiveByReturnRequestId(
            @Param("returnRequestId") UUID returnRequestId);
}
