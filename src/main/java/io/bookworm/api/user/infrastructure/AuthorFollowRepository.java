package io.bookworm.api.user.infrastructure;

import io.bookworm.api.user.domain.AuthorFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link AuthorFollow} entities.
 */
@Repository
public interface AuthorFollowRepository extends JpaRepository<AuthorFollow, UUID> {

    /**
     * Finds the active follow relationship between a member and an author.
     * Returns empty if not following or the follow was soft-deleted (unfollowed).
     */
    @Query("""
           SELECT f FROM AuthorFollow f
           WHERE f.member.memberId = :memberId
             AND f.author.authorId = :authorId
             AND f.deletedAt IS NULL
           """)
    Optional<AuthorFollow> findActiveFollow(
            @Param("memberId") UUID memberId,
            @Param("authorId") UUID authorId);

    /**
     * Returns all active follows for a member (for the "followed authors" list).
     */
    @Query("""
           SELECT f FROM AuthorFollow f
           WHERE f.member.memberId = :memberId
             AND f.deletedAt IS NULL
           ORDER BY f.followedAt DESC
           """)
    List<AuthorFollow> findActiveByMemberId(@Param("memberId") UUID memberId);
}
