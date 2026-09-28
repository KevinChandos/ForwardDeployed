package io.bookworm.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Registration confirmation response DTO.
 * <p>
 * Why: Returns created member identifier and account metadata following successful registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private UUID memberId;
    private String email;
    private String firstName;
    private String lastName;
    private String message;
}
