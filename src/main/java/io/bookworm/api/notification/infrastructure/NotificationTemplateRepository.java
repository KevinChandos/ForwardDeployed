package io.bookworm.api.notification.infrastructure;

import io.bookworm.api.notification.domain.NotificationTemplate;
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

/**
 * JPA repository for {@link NotificationTemplate} entities.
 * <p>
 * Why: accesses notification templates by natural primary key string (`templateCode`),
 * supporting template administration, active channel lookups, and dynamic specifications.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String>, JpaSpecificationExecutor<NotificationTemplate> {

    /**
     * Finds an active template by its template code.
     */
    @Query("""
           SELECT t FROM NotificationTemplate t
           WHERE t.templateCode = :code
             AND t.isActive = true
             AND t.deletedAt IS NULL
           """)
    Optional<NotificationTemplate> findActiveByCode(@Param("code") String code);

    /**
     * Returns all active templates for a given delivery channel.
     */
    @Query("""
           SELECT t FROM NotificationTemplate t
           WHERE t.channel = :channel
             AND t.isActive = true
             AND t.deletedAt IS NULL
           ORDER BY t.templateCode ASC
           """)
    List<NotificationTemplate> findActiveByChannel(@Param("channel") NotificationChannel channel);

    /**
     * Pages all templates with soft-delete filtering.
     */
    @Query(value = "SELECT t FROM NotificationTemplate t WHERE t.deletedAt IS NULL ORDER BY t.templateCode ASC",
           countQuery = "SELECT COUNT(t) FROM NotificationTemplate t WHERE t.deletedAt IS NULL")
    Page<NotificationTemplate> findAllActive(Pageable pageable);

    /**
     * Checks whether an active template exists with the given code.
     */
    @Query("SELECT COUNT(t) > 0 FROM NotificationTemplate t WHERE t.templateCode = :code AND t.deletedAt IS NULL")
    boolean existsActiveByCode(@Param("code") String code);
}
