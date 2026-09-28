package io.bookworm.api.outbox.infrastructure;

import io.bookworm.api.outbox.domain.DomainEventOutbox;
import io.bookworm.api.outbox.domain.DomainEventOutbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link DomainEventOutbox} entities.
 * <p>
 * Why: the relay process needs an efficient way to claim batches of PENDING events.
 * The bulk UPDATE approach (claim then process) is safer than SELECT + individual
 * UPDATE because it reduces the window for duplicate delivery under concurrent
 * relay instances. A separate relay instance ID column could further reduce
 * collision — omitted here as the platform starts with a single relay process.
 */
@Repository
public interface DomainEventOutboxRepository extends JpaRepository<DomainEventOutbox, UUID> {

    /**
     * Returns a batch of PENDING events ordered by {@code occurredAt} for relay.
     * The relay process claims this batch, publishes each event, then marks them
     * PUBLISHED (or FAILED) in the same transaction.
     */
    @Query("""
           SELECT e FROM DomainEventOutbox e
           WHERE e.status = 'PENDING'
           ORDER BY e.occurredAt ASC
           """)
    List<DomainEventOutbox> findPendingBatch(Pageable pageable);

    /**
     * Bulk-marks a list of events as PUBLISHED after successful relay.
     * Uses a single UPDATE to minimise round-trips.
     */
    @Modifying
    @Query("""
           UPDATE DomainEventOutbox e
           SET e.status = 'PUBLISHED', e.publishedAt = CURRENT_TIMESTAMP
           WHERE e.eventId IN :ids
           """)
    void markAsPublished(@Param("ids") List<UUID> ids);

    /**
     * Bulk-increments retry count and marks events as FAILED when they exceed
     * the retry threshold. Called by the relay error handler.
     */
    @Modifying
    @Query("""
           UPDATE DomainEventOutbox e
           SET e.retryCount = e.retryCount + 1,
               e.status = CASE WHEN (e.retryCount + 1) >= :maxRetries THEN 'FAILED' ELSE 'PENDING' END
           WHERE e.eventId IN :ids
           """)
    void incrementRetryCount(
            @Param("ids") List<UUID> ids,
            @Param("maxRetries") int maxRetries);
}
