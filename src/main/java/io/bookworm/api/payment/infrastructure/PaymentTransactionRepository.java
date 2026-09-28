package io.bookworm.api.payment.infrastructure;

import io.bookworm.api.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link PaymentTransaction} entities.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /** Finds the active (non-soft-deleted) transaction for an order. */
    @Query("""
           SELECT t FROM PaymentTransaction t
           WHERE t.order.orderId = :orderId
             AND t.deletedAt IS NULL
           ORDER BY t.createdAt DESC
           """)
    Optional<PaymentTransaction> findLatestByOrderId(@Param("orderId") UUID orderId);

    /**
     * Finds a transaction by the gateway's own transaction ID.
     * Used to correlate incoming webhook events with internal records.
     */
    @Query("""
           SELECT t FROM PaymentTransaction t
           WHERE t.gatewayId = :gatewayId
             AND t.deletedAt IS NULL
           """)
    Optional<PaymentTransaction> findActiveByGatewayId(@Param("gatewayId") String gatewayId);
}
