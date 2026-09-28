package io.bookworm.api.payment.infrastructure;

import io.bookworm.api.payment.domain.Refund;
import io.bookworm.api.payment.domain.Refund.RefundStatus;
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
 * JPA repository for {@link Refund} entities.
 * <p>
 * Why: queries refunds associated with payment transactions and orders.
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID>, JpaSpecificationExecutor<Refund> {

    /**
     * Finds an active refund by ID.
     */
    @Query("SELECT r FROM Refund r WHERE r.refundId = :id AND r.deletedAt IS NULL")
    Optional<Refund> findActiveById(@Param("id") UUID id);

    /**
     * Finds all active refunds for a given payment transaction.
     */
    @Query("""
           SELECT r FROM Refund r
           WHERE r.transaction.transactionId = :transactionId
             AND r.deletedAt IS NULL
           ORDER BY r.initiatedAt DESC
           """)
    List<Refund> findActiveByTransactionId(@Param("transactionId") UUID transactionId);

    /**
     * Finds all active refunds for an order directly via denormalised order_id.
     */
    @Query("""
           SELECT r FROM Refund r
           WHERE r.order.orderId = :orderId
             AND r.deletedAt IS NULL
           ORDER BY r.initiatedAt DESC
           """)
    List<Refund> findActiveByOrderId(@Param("orderId") UUID orderId);

    /**
     * Pages refunds by status.
     */
    @Query(value = "SELECT r FROM Refund r WHERE r.status = :status AND r.deletedAt IS NULL",
           countQuery = "SELECT COUNT(r) FROM Refund r WHERE r.status = :status AND r.deletedAt IS NULL")
    Page<Refund> findByStatus(@Param("status") RefundStatus status, Pageable pageable);

    /**
     * Finds refund by gateway refund ID.
     */
    @Query("SELECT r FROM Refund r WHERE r.gatewayRefundId = :gatewayRefundId AND r.deletedAt IS NULL")
    Optional<Refund> findByGatewayRefundId(@Param("gatewayRefundId") String gatewayRefundId);
}
