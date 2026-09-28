package io.bookworm.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT access and refresh token response DTO.
 * <p>
 * Why: Transports newly generated Bearer JWT token, refresh token, expiry TTL, and member summary to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private MemberSummaryDTO member;
}
