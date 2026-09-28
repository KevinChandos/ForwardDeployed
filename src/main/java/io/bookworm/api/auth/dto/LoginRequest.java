package io.bookworm.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication login request payload DTO.
 * <p>
 * Why: Supports authentication with either email address or E.164 phone number via generic identifier field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Identifier (email or phone) is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
