package io.bookworm.api.common.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base class for every auditable entity in Book Worm.
 * <p>
 * Why: centralises the six audit columns (created_at, updated_at, created_by,
 * updated_by, deleted_at, version) and the optimistic-lock counter so no entity
 * needs to declare them individually.
 * <p>
 * Side effects: {@code @EntityListeners(AuditingEntityListener.class)} requires
 * {@code @EnableJpaAuditing} on {@link io.bookworm.api.BookwormApplication}.
 * The {@code deleted_at} column implements soft-delete; queries must always
 * include {@code WHERE deleted_at IS NULL} unless explicitly reading deleted rows.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

    /**
     * Timestamp when the row was first persisted.
     * Populated automatically by Spring Data auditing; never updated after insert.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp of the last modification.
     * Updated on every merge/flush cycle by the AuditingEntityListener.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * UUID of the member or system actor who created the row.
     * NULL is allowed for system-generated rows (e.g. outbox events).
     */
    @CreatedBy
    @Column(name = "created_by")
    private UUID createdBy;

    /**
     * UUID of the member or system actor who last modified the row.
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Soft-delete marker. NULL means the row is active.
     * Setting this to a non-null timestamp logically deletes the row.
     * Hard deletes are never performed on production data.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Optimistic-lock counter. Hibernate increments this on every UPDATE.
     * The application layer passes the client-supplied version into the entity
     * before calling save(); a mismatch causes an OptimisticLockException
     * which is translated to HTTP 409 OPTIMISTIC_LOCK_CONFLICT.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Convenience method for soft-delete; sets {@code deletedAt} to now.
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    /**
     * Returns true when this row has been soft-deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
