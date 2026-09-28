package io.bookworm.api.checkout.infrastructure;

import io.bookworm.api.checkout.domain.CheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link CheckoutSession} entities.
 */
@Repository
public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {

    /** Finds an active, non-expired checkout session by its ID. */
    @Query("""
           SELECT s FROM CheckoutSession s
           WHERE s.sessionId = :sessionId
             AND s.deletedAt IS NULL
             AND s.status NOT IN ('COMPLETED', 'EXPIRED')
             AND s.expiresAt > CURRENT_TIMESTAMP
           """)
    Optional<CheckoutSession> findActiveById(@Param("sessionId") UUID sessionId);

    /**
     * Finds an active checkout session for a cart.
     * Used to prevent creating a second session for the same cart.
     */
    @Query("""
           SELECT s FROM CheckoutSession s
           WHERE s.cart.cartId = :cartId
             AND s.deletedAt IS NULL
             AND s.status NOT IN ('COMPLETED', 'EXPIRED')
             AND s.expiresAt > CURRENT_TIMESTAMP
           """)
    Optional<CheckoutSession> findActiveByCartId(@Param("cartId") UUID cartId);
}
