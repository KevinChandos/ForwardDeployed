package io.bookworm.api.auth.infrastructure;

import io.bookworm.api.auth.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Session} entities.
 * <p>
 * Why: session lookups by token hash are on the hot authentication path; the
 * index on {@code access_token_hash} makes these O(1). Logout is a soft-delete
 * performed via a bulk JPQL UPDATE to avoid loading the entity.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Finds an active (non-expired, non-deleted) session by its hashed access token.
     * Called on every authenticated request to validate the token.
     */
    @Query("""
           SELECT s FROM Session s
           WHERE s.accessTokenHash = :hash
             AND s.deletedAt IS NULL
             AND s.expiresAt > CURRENT_TIMESTAMP
           """)
    Optional<Session> findActiveByAccessTokenHash(@Param("hash") String hash);

    /**
     * Finds an active session by its hashed refresh token.
     * Called during token refresh to validate and rotate the refresh token.
     */
    @Query("""
           SELECT s FROM Session s
           WHERE s.refreshTokenHash = :hash
             AND s.deletedAt IS NULL
             AND s.expiresAt > CURRENT_TIMESTAMP
           """)
    Optional<Session> findActiveByRefreshTokenHash(@Param("hash") String hash);

    /**
     * Soft-deletes all active sessions for a member (global logout).
     * Uses a bulk JPQL UPDATE to avoid loading entities.
     */
    @Modifying
    @Query("""
           UPDATE Session s
           SET s.deletedAt = CURRENT_TIMESTAMP
           WHERE s.member.memberId = :memberId
             AND s.deletedAt IS NULL
           """)
    void softDeleteAllByMemberId(@Param("memberId") UUID memberId);
}
