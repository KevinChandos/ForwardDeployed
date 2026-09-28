package io.bookworm.api.notification.infrastructure;

import io.bookworm.api.notification.domain.NotificationEvent;
import io.bookworm.api.notification.domain.NotificationEvent.NotificationStatus;
import io.bookworm.api.notification.domain.NotificationTemplate.NotificationChannel;
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
 * JPA repository for {@link NotificationEvent} entities.
 * <p>
 * Why: supports worker polling for pending notification dispatches, failure inspection,
 * retry queues, member notification history queries, and specifications for administrative filtering.
 */
@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID>, JpaSpecificationExecutor<NotificationEvent> {

    /**
     * Finds an active notification event by ID.
     */
    @Query("SELECT n FROM NotificationEvent n WHERE n.notificationId = :id AND n.deletedAt IS NULL")
    Optional<NotificationEvent> findActiveById(@Param("id") UUID id);

    /**
     * Finds queued/pending notification events to be dispatched by background workers.
     * Ordered oldest first for FIFO processing.
     */
    @Query("""
           SELECT n FROM NotificationEvent n
           WHERE n.status = :status
             AND n.deletedAt IS NULL
           ORDER BY n.createdAt ASC
           """)
    List<NotificationEvent> findTopPendingByStatus(
            @Param("status") NotificationStatus status,
            Pageable pageable);

    /**
     * Pages all notification events sent or queued for a specific member.
     */
    @Query(value = """
           SELECT n FROM NotificationEvent n
           WHERE n.recipientMember.memberId = :memberId
             AND n.deletedAt IS NULL
           ORDER BY n.createdAt DESC
           """,
           countQuery = """
           SELECT COUNT(n) FROM NotificationEvent n
           WHERE n.recipientMember.memberId = :memberId
             AND n.deletedAt IS NULL
           """)
    Page<NotificationEvent> findActiveByRecipientMemberId(
            @Param("memberId") UUID memberId,
            Pageable pageable);

    /**
     * Pages all notification events for an external guest recipient email.
     */
    @Query(value = """
           SELECT n FROM NotificationEvent n
           WHERE n.recipientEmail = :email
             AND n.deletedAt IS NULL
           ORDER BY n.createdAt DESC
           """,
           countQuery = """
           SELECT COUNT(n) FROM NotificationEvent n
           WHERE n.recipientEmail = :email
             AND n.deletedAt IS NULL
           """)
    Page<NotificationEvent> findActiveByRecipientEmail(
            @Param("email") String email,
            Pageable pageable);

    /**
     * Pages notifications by delivery status and channel.
     */
    @Query(value = """
           SELECT n FROM NotificationEvent n
           WHERE n.status = :status
             AND n.channel = :channel
             AND n.deletedAt IS NULL
           ORDER BY n.createdAt DESC
           """,
           countQuery = """
           SELECT COUNT(n) FROM NotificationEvent n
           WHERE n.status = :status
             AND n.channel = :channel
             AND n.deletedAt IS NULL
           """)
    Page<NotificationEvent> findByStatusAndChannel(
            @Param("status") NotificationStatus status,
            @Param("channel") NotificationChannel channel,
            Pageable pageable);
}
