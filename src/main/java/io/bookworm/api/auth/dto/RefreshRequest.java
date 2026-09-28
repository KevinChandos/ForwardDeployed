package io.bookworm.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token refresh request payload DTO.
 * <p>
 * Why: Accepts a cryptographically valid refresh token to rotate authentication credentials and issue fresh JWTs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
