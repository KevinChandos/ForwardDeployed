package io.bookworm.api.shipping.infrastructure;

import io.bookworm.api.shipping.domain.ShipmentEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link ShipmentEvent} entities.
 * <p>
 * Why: queries append-only tracking events in a shipment's timeline.
 */
@Repository
public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, UUID>, JpaSpecificationExecutor<ShipmentEvent> {

    /**
     * Finds all tracking events for a shipment ordered chronologically by occurredAt.
     */
    @Query("""
           SELECT e FROM ShipmentEvent e
           WHERE e.shipment.shipmentId = :shipmentId
             AND e.deletedAt IS NULL
           ORDER BY e.occurredAt ASC
           """)
    List<ShipmentEvent> findActiveByShipmentId(@Param("shipmentId") UUID shipmentId);

    /**
     * Pages all tracking events for a shipment.
     */
    @Query(value = """
           SELECT e FROM ShipmentEvent e
           WHERE e.shipment.shipmentId = :shipmentId
             AND e.deletedAt IS NULL
           ORDER BY e.occurredAt DESC
           """,
           countQuery = """
           SELECT COUNT(e) FROM ShipmentEvent e
           WHERE e.shipment.shipmentId = :shipmentId
             AND e.deletedAt IS NULL
           """)
    Page<ShipmentEvent> findActiveByShipmentId(@Param("shipmentId") UUID shipmentId, Pageable pageable);
}
