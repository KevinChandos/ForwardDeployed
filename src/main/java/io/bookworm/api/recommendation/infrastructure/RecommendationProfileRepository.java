package io.bookworm.api.recommendation.infrastructure;

import io.bookworm.api.recommendation.domain.RecommendationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link RecommendationProfile} entities.
 */
@Repository
public interface RecommendationProfileRepository extends JpaRepository<RecommendationProfile, UUID> {

    /** Finds the active recommendation profile for a member. */
    @Query("""
           SELECT p FROM RecommendationProfile p
           WHERE p.member.memberId = :memberId
             AND p.deletedAt IS NULL
           """)
    Optional<RecommendationProfile> findActiveByMemberId(@Param("memberId") UUID memberId);

    /**
     * Finds the active recommendation profile for a guest token.
     * Used when a guest accesses personalised recommendation endpoints.
     */
    @Query("""
           SELECT p FROM RecommendationProfile p
           WHERE p.guestToken = :guestToken
             AND p.deletedAt IS NULL
           """)
    Optional<RecommendationProfile> findActiveByGuestToken(@Param("guestToken") String guestToken);
}
