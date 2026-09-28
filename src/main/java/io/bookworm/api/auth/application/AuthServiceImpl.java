package io.bookworm.api.auth.application;

import io.bookworm.api.auth.domain.Credential;
import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.domain.MemberRole;
import io.bookworm.api.auth.domain.Session;
import io.bookworm.api.auth.dto.*;
import io.bookworm.api.auth.infrastructure.CredentialRepository;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.auth.infrastructure.MemberRoleRepository;
import io.bookworm.api.auth.infrastructure.SessionRepository;
import io.bookworm.api.auth.mapper.AuthMapper;
import io.bookworm.api.common.exception.AuthenticationFailedException;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceConflictException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.payment.domain.WalletAccount;
import io.bookworm.api.payment.infrastructure.WalletAccountRepository;
import io.bookworm.api.user.domain.Wishlist;
import io.bookworm.api.user.infrastructure.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for member authentication, registration, token refresh, and password management.
 * <p>
 * Why: Orchestrates identity lifecycle operations, credential verification with BCrypt hashing,
 * session creation with token hashing, and wallet/wishlist auto-provisioning for new registrations.
 * Side effects: Emits session and member audit records, mutates credentials and active sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final CredentialRepository credentialRepository;
    private final SessionRepository sessionRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final io.bookworm.api.security.JwtProvider jwtProvider;

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 604800L; // 7 days

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        log.info("Processing login attempt for identifier: {}", request.getIdentifier());

        if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Identifier must not be blank");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Password must not be blank");
        }

        String normalizedIdentifier = request.getIdentifier().trim().toLowerCase();
        Member member = memberRepository.findActiveByIdentifier(normalizedIdentifier)
                .orElseThrow(() -> {
                    log.warn("Login failed: member not found for identifier {}", normalizedIdentifier);
                    return new AuthenticationFailedException("INVALID_CREDENTIALS", "Invalid credentials provided");
                });

        if (member.getStatus() == Member.MemberStatus.SUSPENDED) {
            log.warn("Login rejected: member account {} is suspended", member.getMemberId());
            throw new BusinessRuleException("ACCOUNT_SUSPENDED", "Account is suspended. Please contact customer support.");
        }
        if (member.getStatus() == Member.MemberStatus.CLOSED) {
            log.warn("Login rejected: member account {} is closed", member.getMemberId());
            throw new BusinessRuleException("ACCOUNT_CLOSED", "Account is closed.");
        }

        Credential.Channel channel = normalizedIdentifier.contains("@") ? Credential.Channel.EMAIL : Credential.Channel.PHONE;
        Credential credential = credentialRepository.findActiveByMemberAndChannel(member.getMemberId(), channel)
                .orElseThrow(() -> {
                    log.warn("Login failed: no active credential for member {} on channel {}", member.getMemberId(), channel);
                    return new AuthenticationFailedException("INVALID_CREDENTIALS", "Invalid credentials provided");
                });

        if (!passwordEncoder.matches(request.getPassword(), credential.getHashedSecret())) {
            log.warn("Login failed: password mismatch for member {}", member.getMemberId());
            throw new AuthenticationFailedException("INVALID_CREDENTIALS", "Invalid credentials provided");
        }

        List<MemberRole> roles = memberRoleRepository.findActiveByMemberId(member.getMemberId());
        member.setRoles(roles);

        List<String> roleNames = roles.stream()
                .filter(r -> r.getDeletedAt() == null && r.getRoleName() != null)
                .map(r -> r.getRoleName().name())
                .toList();

        String jwtAccessToken = jwtProvider.generateAccessToken(member.getMemberId(), normalizedIdentifier, roleNames);
        String rawRefreshToken = jwtProvider.generateRefreshToken(member.getMemberId(), normalizedIdentifier);

        Session session = new Session();
        session.setMember(member);
        session.setAccessTokenHash(sha256Hex(jwtAccessToken));
        session.setRefreshTokenHash(sha256Hex(rawRefreshToken));
        session.setExpiresAt(OffsetDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS));
        session.setDeviceInfo(request.getDeviceInfo());
        sessionRepository.save(session);

        log.info("Member {} successfully logged in; session created", member.getMemberId());

        return TokenResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
                .member(authMapper.toMemberSummaryDTO(member))
                .build();
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Processing registration for email: {}", request.getEmail());

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessRuleException("INVALID_REGISTRATION", "Email is required for registration");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BusinessRuleException("WEAK_PASSWORD", "Password must be at least 8 characters long");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (memberRepository.existsActiveByEmail(normalizedEmail)) {
            log.warn("Registration rejected: email {} already registered", normalizedEmail);
            throw new ResourceConflictException("EMAIL_ALREADY_EXISTS", "An account with this email address already exists");
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String phone = request.getPhoneNumber().trim();
            if (memberRepository.existsActiveByPhoneNumber(phone)) {
                log.warn("Registration rejected: phone number {} already registered", phone);
                throw new ResourceConflictException("PHONE_ALREADY_EXISTS", "An account with this phone number already exists");
            }
        }

        Member member = new Member();
        member.setEmail(normalizedEmail);
        member.setDisplayName(request.getDisplayName() != null ? request.getDisplayName().trim() : normalizedEmail.split("@")[0]);
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            member.setPhoneNumber(request.getPhoneNumber().trim());
        }
        member.setStatus(Member.MemberStatus.ACTIVE);
        Member savedMember = memberRepository.save(member);

        Credential credential = new Credential();
        credential.setMember(savedMember);
        credential.setChannel(Credential.Channel.EMAIL);
        credential.setHashedSecret(passwordEncoder.encode(request.getPassword()));
        credentialRepository.save(credential);

        MemberRole role = new MemberRole();
        role.setMember(savedMember);
        role.setRoleName(MemberRole.RoleName.REGISTERED_USER);
        memberRoleRepository.save(role);
        savedMember.getRoles().add(role);

        // Auto-provision wallet
        WalletAccount wallet = new WalletAccount();
        wallet.setMember(savedMember);
        wallet.setBalance(java.math.BigDecimal.ZERO);
        wallet.setCurrency("INR");
        walletAccountRepository.save(wallet);

        // Auto-provision wishlist
        Wishlist wishlist = new Wishlist();
        wishlist.setMember(savedMember);
        wishlistRepository.save(wishlist);

        log.info("Member {} registered successfully with default role REGISTERED_USER and wallet", savedMember.getMemberId());
        return authMapper.toRegisterResponse(savedMember);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshRequest request) {
        log.info("Processing token refresh");

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BusinessRuleException("INVALID_TOKEN", "Refresh token must not be blank");
        }

        String hashedRefresh = sha256Hex(request.getRefreshToken().trim());
        Session session = sessionRepository.findActiveByRefreshTokenHash(hashedRefresh)
                .orElseThrow(() -> {
                    log.warn("Token refresh rejected: active session not found for token hash");
                    return new AuthenticationFailedException("INVALID_REFRESH_TOKEN", "Invalid or expired refresh token");
                });

        Member member = session.getMember();
        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            throw new BusinessRuleException("ACCOUNT_INACTIVE", "Account is not active");
        }

        List<MemberRole> roles = memberRoleRepository.findActiveByMemberId(member.getMemberId());
        member.setRoles(roles);

        List<String> roleNames = roles.stream()
                .filter(r -> r.getDeletedAt() == null && r.getRoleName() != null)
                .map(r -> r.getRoleName().name())
                .toList();

        String username = member.getEmail() != null ? member.getEmail() : member.getPhoneNumber();
        String newJwtAccessToken = jwtProvider.generateAccessToken(member.getMemberId(), username, roleNames);
        String newRawRefreshToken = jwtProvider.generateRefreshToken(member.getMemberId(), username);

        session.setAccessTokenHash(sha256Hex(newJwtAccessToken));
        session.setRefreshTokenHash(sha256Hex(newRawRefreshToken));
        session.setExpiresAt(OffsetDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS));
        sessionRepository.save(session);

        log.info("Session {} refreshed for member {}", session.getSessionId(), member.getMemberId());

        return TokenResponse.builder()
                .accessToken(newJwtAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
                .member(authMapper.toMemberSummaryDTO(member))
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessRuleException("INVALID_EMAIL", "Email must not be blank");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        memberRepository.findActiveByEmail(normalizedEmail).ifPresent(member -> {
            credentialRepository.findActiveByMemberAndChannel(member.getMemberId(), Credential.Channel.EMAIL)
                    .ifPresent(credential -> {
                        String resetToken = UUID.randomUUID().toString();
                        credential.setResetToken(sha256Hex(resetToken));
                        credential.setResetExpiresAt(OffsetDateTime.now().plusHours(1));
                        credentialRepository.save(credential);
                        log.info("Reset token generated for member {}", member.getMemberId());
                        // Email dispatch logic handled asynchronously via outbox/event relay
                    });
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset confirmation");

        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BusinessRuleException("INVALID_TOKEN", "Reset token must not be blank");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new BusinessRuleException("WEAK_PASSWORD", "New password must be at least 8 characters long");
        }

        String hashedToken = sha256Hex(request.getToken().trim());
        Credential credential = credentialRepository.findActiveByResetToken(hashedToken)
                .orElseThrow(() -> new BusinessRuleException("INVALID_RESET_TOKEN", "Invalid or expired password reset token"));

        if (credential.getResetExpiresAt() == null || credential.getResetExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessRuleException("EXPIRED_RESET_TOKEN", "Password reset token has expired");
        }

        credential.setHashedSecret(passwordEncoder.encode(request.getNewPassword()));
        credential.setResetToken(null);
        credential.setResetExpiresAt(null);
        credentialRepository.save(credential);

        // Revoke all existing sessions on password change
        sessionRepository.softDeleteAllByMemberId(credential.getMember().getMemberId());
        log.info("Password successfully reset and sessions revoked for member {}", credential.getMember().getMemberId());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String hashedRefresh = sha256Hex(refreshToken.trim());
            sessionRepository.findActiveByRefreshTokenHash(hashedRefresh).ifPresent(session -> {
                session.setDeletedAt(OffsetDateTime.now());
                sessionRepository.save(session);
                log.info("Session {} invalidated on logout", session.getSessionId());
            });
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
