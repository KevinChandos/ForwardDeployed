package io.bookworm.api.security;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.domain.MemberRole;
import io.bookworm.api.auth.infrastructure.CredentialRepository;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.auth.infrastructure.MemberRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom {@link UserDetailsService} implementation that loads member identity from the database.
 * <p>
 * Why: Required by Spring Security AuthenticationManager to authenticate credentials during login
 * and bridge database Member/MemberRole entities with Spring Security's UserDetails model.
 * <p>
 * Side effects: Queries member, credential, and role repositories during authentication lookups.
 */
@Service
@RequiredArgsConstructor
public class BookwormUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final CredentialRepository credentialRepository;
    private final MemberRoleRepository memberRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Identifier must not be blank");
        }

        String normalized = identifier.trim().toLowerCase();
        Member member = memberRepository.findActiveByIdentifier(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found with identifier: " + normalized));

        List<MemberRole> roles = memberRoleRepository.findActiveByMemberId(member.getMemberId());
        Collection<? extends GrantedAuthority> authorities = roles.stream()
                .map(r -> {
                    String name = r.getRoleName() != null ? r.getRoleName().name() : "GUEST";
                    return name.startsWith("ROLE_") ? name : "ROLE_" + name;
                })
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        boolean nonLocked = member.getStatus() != Member.MemberStatus.SUSPENDED;
        boolean enabled = member.getStatus() != Member.MemberStatus.CLOSED;

        return new BookwormUserDetails(
                member.getMemberId(),
                member.getEmail() != null ? member.getEmail() : member.getPhoneNumber(),
                "", // Password verified in service layer via Credential entity
                authorities,
                nonLocked,
                enabled
        );
    }
}
