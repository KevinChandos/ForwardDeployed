package io.bookworm.api.payment.infrastructure;

import io.bookworm.api.payment.domain.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link WalletAccount} entities.
 */
@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {

    /** Finds the active wallet for a member. */
    @Query("""
           SELECT w FROM WalletAccount w
           WHERE w.member.memberId = :memberId
             AND w.deletedAt IS NULL
           """)
    Optional<WalletAccount> findActiveByMemberId(@Param("memberId") UUID memberId);
}
