package io.bookworm.api.checkout.infrastructure;

import io.bookworm.api.checkout.domain.CheckoutAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link CheckoutAddress} entities.
 * <p>
 * Why: retrieves the delivery address configured during an active checkout session.
 */
@Repository
public interface CheckoutAddressRepository extends JpaRepository<CheckoutAddress, UUID>, JpaSpecificationExecutor<CheckoutAddress> {

    /**
     * Finds the active delivery address for a given checkout session.
     */
    @Query("""
           SELECT a FROM CheckoutAddress a
           WHERE a.session.sessionId = :sessionId
             AND a.deletedAt IS NULL
           """)
    Optional<CheckoutAddress> findActiveBySessionId(@Param("sessionId") UUID sessionId);
}
