package io.bookworm.api.checkout.infrastructure;

import io.bookworm.api.checkout.domain.OrderDeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link OrderDeliveryAddress} entities.
 * <p>
 * Why: retrieves the immutable snapshot of a delivery address associated with a confirmed order.
 */
@Repository
public interface OrderDeliveryAddressRepository extends JpaRepository<OrderDeliveryAddress, UUID>, JpaSpecificationExecutor<OrderDeliveryAddress> {

    /**
     * Finds the delivery address snapshot for a specific order.
     */
    @Query("""
           SELECT a FROM OrderDeliveryAddress a
           WHERE a.order.orderId = :orderId
             AND a.deletedAt IS NULL
           """)
    Optional<OrderDeliveryAddress> findActiveByOrderId(@Param("orderId") UUID orderId);
}
