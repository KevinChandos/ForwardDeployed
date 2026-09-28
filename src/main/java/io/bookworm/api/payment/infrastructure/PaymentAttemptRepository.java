package io.bookworm.api.payment.infrastructure;

import io.bookworm.api.payment.domain.PaymentAttempt;
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
 * JPA repository for {@link PaymentAttempt} entities.
 * <p>
 * Why: queries append-only attempt logs for payment gateway transactions to support reconciliation and fraud analysis.
 */
@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID>, JpaSpecificationExecutor<PaymentAttempt> {

    /**
     * Finds all active attempts for a given transaction ordered chronologically.
     */
    @Query("""
           SELECT a FROM PaymentAttempt a
           WHERE a.transaction.transactionId = :transactionId
             AND a.deletedAt IS NULL
           ORDER BY a.attemptedAt ASC
           """)
    List<PaymentAttempt> findActiveByTransactionId(@Param("transactionId") UUID transactionId);

    /**
     * Pages all attempts for a given transaction.
     */
    @Query(value = """
           SELECT a FROM PaymentAttempt a
           WHERE a.transaction.transactionId = :transactionId
             AND a.deletedAt IS NULL
           ORDER BY a.attemptedAt DESC
           """,
           countQuery = """
           SELECT COUNT(a) FROM PaymentAttempt a
           WHERE a.transaction.transactionId = :transactionId
             AND a.deletedAt IS NULL
           """)
    Page<PaymentAttempt> findActiveByTransactionId(@Param("transactionId") UUID transactionId, Pageable pageable);
}
