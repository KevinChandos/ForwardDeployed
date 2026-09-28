package io.bookworm.api.auth.infrastructure;

import io.bookworm.api.auth.domain.MemberRole;
import io.bookworm.api.auth.domain.MemberRole.RoleName;
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
 * JPA repository for {@link MemberRole} entities.
 * <p>
 * Why: queries and manages individual role grants for members.
 */
@Repository
public interface MemberRoleRepository extends JpaRepository<MemberRole, UUID>, JpaSpecificationExecutor<MemberRole> {

    /**
     * Finds all active role grants for a member.
     */
    @Query("""
           SELECT r FROM MemberRole r
           WHERE r.member.memberId = :memberId
             AND r.deletedAt IS NULL
           """)
    List<MemberRole> findActiveByMemberId(@Param("memberId") UUID memberId);

    /**
     * Finds a specific role grant for a member.
     */
    @Query("""
           SELECT r FROM MemberRole r
           WHERE r.member.memberId = :memberId
             AND r.roleName = :roleName
             AND r.deletedAt IS NULL
           """)
    Optional<MemberRole> findActiveByMemberIdAndRoleName(
            @Param("memberId") UUID memberId,
            @Param("roleName") RoleName roleName);

    /**
     * Checks if a member possesses an active role.
     */
    @Query("""
           SELECT COUNT(r) > 0 FROM MemberRole r
           WHERE r.member.memberId = :memberId
             AND r.roleName = :roleName
             AND r.deletedAt IS NULL
           """)
    boolean hasRole(
            @Param("memberId") UUID memberId,
            @Param("roleName") RoleName roleName);

    /**
     * Pages all members assigned a specific role.
     */
    @Query(value = """
           SELECT r FROM MemberRole r
           JOIN FETCH r.member m
           WHERE r.roleName = :roleName
             AND r.deletedAt IS NULL
             AND m.deletedAt IS NULL
           """,
           countQuery = """
           SELECT COUNT(r) FROM MemberRole r
           WHERE r.roleName = :roleName
             AND r.deletedAt IS NULL
           """)
    Page<MemberRole> findActiveByRoleName(@Param("roleName") RoleName roleName, Pageable pageable);
}
