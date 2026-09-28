package io.bookworm.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Component responsible for generating, parsing, and validating JSON Web Tokens (JWT).
 * <p>
 * Why: Centralizes all cryptographic operations, claims extraction, and token creation for
 * the platform's stateless authentication mechanism using HMAC-SHA-256 (JJWT 0.12.x).
 * <p>
 * Side effects: Emits log entries on malformed, expired, or unsupported JWT tokens.
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey signingKey;
    private final long accessTokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;

    public JwtProvider(
            @Value("${spring.security.jwt.secret:CHANGE_ME_IN_PRODUCTION_USE_256_BIT_KEY_AT_LEAST_32_CHARS}") String secret,
            @Value("${spring.security.jwt.access-token-ttl-seconds:900}") long accessTokenValidityInSeconds,
            @Value("${spring.security.jwt.refresh-token-ttl-seconds:604800}") long refreshTokenValidityInSeconds) {
        
        // Pad secret if shorter than 256 bits (32 bytes) to ensure HMAC-SHA-256 compliance
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.signingKey = Keys.hmacShaKeyFor(padded);
        } else {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
    }

    /**
     * Generates a signed access token containing subject, memberId UUID, and assigned roles.
     *
     * @param memberId unique identifier of the member
     * @param username email or phone identifier
     * @param roles list of assigned roles (e.g., GUEST, CUSTOMER, ADMIN, etc.)
     * @return signed JWT string
     */
    public String generateAccessToken(UUID memberId, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValidityInSeconds);

        return Jwts.builder()
                .subject(username)
                .claim("memberId", memberId != null ? memberId.toString() : null)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a refresh token with longer expiration TTL.
     *
     * @param memberId unique identifier of the member
     * @param username email or phone identifier
     * @return signed refresh JWT string
     */
    public String generateRefreshToken(UUID memberId, String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenValidityInSeconds);

        return Jwts.builder()
                .subject(username)
                .claim("memberId", memberId != null ? memberId.toString() : null)
                .claim("token_type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates signature and expiration of the provided JWT token.
     *
     * @param token Bearer JWT token string
     * @return true if token is valid and unexpired; false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts Claims payload from a signed JWT token.
     *
     * @param token signed JWT string
     * @return Claims payload
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts username/subject from JWT.
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts member UUID from JWT claims.
     */
    public UUID getMemberId(String token) {
        String memberIdStr = getClaims(token).get("memberId", String.class);
        return memberIdStr != null ? UUID.fromString(memberIdStr) : null;
    }

    /**
     * Reconstructs an {@link Authentication} object from JWT claims for the SecurityContext.
     * Maps roles into Spring Security authorities with ROLE_ prefix.
     */
    @SuppressWarnings("unchecked")
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String username = claims.getSubject();
        String memberIdStr = claims.get("memberId", String.class);
        UUID memberId = memberIdStr != null ? UUID.fromString(memberIdStr) : null;

        List<String> roles = claims.get("roles", List.class);
        Collection<? extends GrantedAuthority> authorities = roles == null
                ? Collections.emptyList()
                : roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        BookwormUserDetails userDetails = new BookwormUserDetails(
                memberId,
                username,
                "", // Password not stored in token
                authorities,
                true,
                true
        );

        return new UsernamePasswordAuthenticationToken(userDetails, token, authorities);
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInSeconds;
    }
}
