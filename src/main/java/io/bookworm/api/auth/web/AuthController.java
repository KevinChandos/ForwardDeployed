package io.bookworm.api.auth.web;

import io.bookworm.api.auth.application.AuthService;
import io.bookworm.api.auth.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for member authentication: registration, login, token refresh,
 * logout, and password reset workflows.
 * <p>
 * Why: Maps the /auth/* OpenAPI paths to {@link AuthService}, keeping HTTP concerns
 * (status codes, headers) out of the service layer.
 */
@Tag(name = "Authentication", description = "Register, login, token refresh, logout, password reset")
@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Operation(
            operationId = "registerMember",
            summary = "Register a new member account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email or phone already registered"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerMember(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Operation(
            operationId = "loginMember",
            summary = "Authenticate with identifier and password"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "422", description = "Account suspended or locked"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginMember(
            @Valid @RequestBody LoginRequest request) {

        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ── POST /auth/refresh ────────────────────────────────────────────────────

    @Operation(
            operationId = "refreshToken",
            summary = "Exchange refresh token for new access token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @Valid @RequestBody RefreshRequest request) {

        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    // ── POST /auth/logout ─────────────────────────────────────────────────────

    @Operation(
            operationId = "logoutMember",
            summary = "Invalidate the current session",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutMember(
            @AuthenticationPrincipal UserDetails principal) {

        // Why: the refresh token is carried in the security context's credentials;
        // the service invalidates the matching session row.
        authService.logout(principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    // ── POST /auth/password/forgot ────────────────────────────────────────────

    @Operation(
            operationId = "forgotPassword",
            summary = "Request a password reset token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Reset request accepted"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        // Why: always returns 202 regardless of whether the identifier exists,
        // preventing user enumeration attacks (documented in OpenAPI spec).
        authService.forgotPassword(request);
        return ResponseEntity.accepted().build();
    }

    // ── POST /auth/password/reset ─────────────────────────────────────────────

    @Operation(
            operationId = "resetPassword",
            summary = "Reset password using reset token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Token expired or already used")
    })
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
