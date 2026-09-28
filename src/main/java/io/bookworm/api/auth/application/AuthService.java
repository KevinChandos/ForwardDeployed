package io.bookworm.api.auth.application;

import io.bookworm.api.auth.dto.*;

/**
 * Service interface for member authentication, registration, token refresh, and password management.
 * <p>
 * Why: Encapsulates authentication lifecycle operations, credential verification, session management,
 * and JWT token minting/revocation behind a clean application boundary.
 */
public interface AuthService {

    /**
     * Authenticates a user using email/phone and password, issuing access and refresh tokens.
     */
    TokenResponse login(LoginRequest request);

    /**
     * Registers a new member account with role REGISTERED_USER and returns registration result with tokens.
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Refreshes access and refresh tokens using an active refresh token.
     */
    TokenResponse refreshToken(RefreshRequest request);

    /**
     * Generates a password reset token for the given email address.
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Resets a member password using a valid reset token.
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Logs out the user by invalidating the active session associated with the refresh token.
     */
    void logout(String refreshToken);
}
