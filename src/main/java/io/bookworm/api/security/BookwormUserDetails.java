package io.bookworm.api.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Spring Security {@link UserDetails} adapter over the Member domain entity.
 * <p>
 * Why: Spring Security needs a {@code UserDetails} object; rather than exposing
 * the JPA entity directly into the security layer, this thin adapter bridges the
 * two without coupling them. The member UUID is carried so that {@link SecurityUtils}
 * can resolve it without a second database round-trip.
 */
@Getter
public class BookwormUserDetails implements UserDetails {

    /** UUID of the authenticated member. */
    private final UUID memberId;

    /** Email or phone used as the authentication identifier. */
    private final String username;

    /** bcrypt/Argon2 hashed password credential. */
    private final String password;

    /** Granted authorities derived from MemberRole records. */
    private final Collection<? extends GrantedAuthority> authorities;

    /** Whether the account is non-expired. */
    private final boolean accountNonExpired;

    /** Whether the account is non-locked (ACTIVE status). */
    private final boolean accountNonLocked;

    /** Credential (password) expiry — always true for this platform. */
    private final boolean credentialsNonExpired;

    /** Whether the account is enabled (not CLOSED). */
    private final boolean enabled;

    public BookwormUserDetails(UUID memberId,
                               String username,
                               String password,
                               Collection<? extends GrantedAuthority> authorities,
                               boolean accountNonLocked,
                               boolean enabled) {
        this.memberId = memberId;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.accountNonExpired = true;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = true;
        this.enabled = enabled;
    }
}
