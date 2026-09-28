package io.bookworm.api.recommendation.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracks a member's or guest's recommendation profile for personalised suggestions.
 * <p>
 * Why: recommendation data is separated from the Member entity to keep the identity
 * bounded context free from discovery concerns. Either {@code member} or
 * {@code guestToken} must be non-null (DB CHECK constraint); the profile is promoted
 * from guest to member when the user authenticates.
 * <p>
 * Side effects: the composite unique constraints on memberId and guestToken
 * (both partial, excluding soft-deleted rows) prevent duplicate profiles.
 */
@Entity
@Table(
    schema = "discovery",
    name = "recommendation_profiles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_rec_profiles_member", columnNames = "member_id"),
        @UniqueConstraint(name = "uq_rec_profiles_guest_token", columnNames = "guest_token")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecommendationProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID profileId;

    /**
     * Associated member. NULL for guest profiles. Unique among active profiles.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",
                foreignKey = @ForeignKey(name = "fk_rec_profiles_member"))
    private Member member;

    /**
     * Stable guest session identifier (from X-Guest-Token header).
     * NULL for authenticated member profiles. Unique among active profiles.
     */
    @Column(name = "guest_token", length = 200)
    private String guestToken;

    /** Personalised book recommendations computed for this profile. */
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecommendedBook> recommendedBooks = new ArrayList<>();
}
