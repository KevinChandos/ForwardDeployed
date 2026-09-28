package io.bookworm.api.auth.infrastructure;

import io.bookworm.api.auth.domain.Credential;
import io.bookworm.api.auth.domain.Credential.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Credential} entities.
 * <p>
 * Why: credential lookups always target a specific member + channel pair to
 * authenticate login attempts or locate the credential to update on password reset.
 * Both finders exclude soft-deleted rows.
 */
@Repository
public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    /**
     * Loads the active credential for a member on a specific channel.
     * Returns empty if the member has no active credential on that channel.
     */
    @Query("""
           SELECT c FROM Credential c
           WHERE c.member.memberId = :memberId
             AND c.channel = :channel
             AND c.deletedAt IS NULL
           """)
    Optional<Credential> findActiveByMemberAndChannel(
            @Param("memberId") UUID memberId,
            @Param("channel") Channel channel);

    /**
     * Loads the active credential for a member that has a matching reset token.
     * Used during the password reset confirmation flow to validate the token.
     */
    @Query("""
           SELECT c FROM Credential c
           WHERE c.resetToken = :token
             AND c.deletedAt IS NULL
           """)
    Optional<Credential> findActiveByResetToken(@Param("token") String token);
}
