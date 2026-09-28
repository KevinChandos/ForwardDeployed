package io.bookworm.api.payment.infrastructure;

import io.bookworm.api.payment.domain.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA repository for {@link WalletTransaction} entities.
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    /**
     * Pages through transaction history for a wallet, most recent first.
     * Used for the wallet statement view.
     */
    @Query(value = """
           SELECT t FROM WalletTransaction t
           WHERE t.wallet.walletId = :walletId
             AND t.deletedAt IS NULL
           ORDER BY t.createdAt DESC
           """,
           countQuery = """
           SELECT COUNT(t) FROM WalletTransaction t
           WHERE t.wallet.walletId = :walletId
             AND t.deletedAt IS NULL
           """)
    Page<WalletTransaction> findActiveByWalletId(
            @Param("walletId") UUID walletId,
            Pageable pageable);
}
