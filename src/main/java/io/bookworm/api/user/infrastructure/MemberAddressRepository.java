package io.bookworm.api.user.infrastructure;

import io.bookworm.api.auth.domain.MemberAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link MemberAddress} entities.
 */
@Repository
public interface MemberAddressRepository extends JpaRepository<MemberAddress, UUID> {

    /** Returns all active addresses for a member, ordered by default flag descending. */
    @Query("""
           SELECT a FROM MemberAddress a
           WHERE a.member.memberId = :memberId
             AND a.deletedAt IS NULL
           ORDER BY a.isDefault DESC, a.createdAt DESC
           """)
    List<MemberAddress> findActiveByMemberId(@Param("memberId") UUID memberId);

    /** Finds a specific active address belonging to a member. */
    @Query("""
           SELECT a FROM MemberAddress a
           WHERE a.addressId = :addressId
             AND a.member.memberId = :memberId
             AND a.deletedAt IS NULL
           """)
    Optional<MemberAddress> findActiveByIdAndMemberId(
            @Param("addressId") UUID addressId,
            @Param("memberId") UUID memberId);
}
