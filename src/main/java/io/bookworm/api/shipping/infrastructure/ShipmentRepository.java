package io.bookworm.api.shipping.infrastructure;

import io.bookworm.api.shipping.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Shipment} entities.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    /** Finds the active shipment for a specific order. */
    @Query("""
           SELECT s FROM Shipment s
           WHERE s.order.orderId = :orderId
             AND s.deletedAt IS NULL
           """)
    Optional<Shipment> findActiveByOrderId(@Param("orderId") UUID orderId);

    /**
     * Finds a shipment with its events eagerly loaded.
     * Used for the tracking response to avoid N+1 when rendering the timeline.
     */
    @Query("""
           SELECT s FROM Shipment s
           LEFT JOIN FETCH s.events e
           WHERE s.shipmentId = :shipmentId
             AND s.deletedAt IS NULL
           ORDER BY e.occurredAt ASC
           """)
    Optional<Shipment> findActiveWithEventsById(@Param("shipmentId") UUID shipmentId);
}
