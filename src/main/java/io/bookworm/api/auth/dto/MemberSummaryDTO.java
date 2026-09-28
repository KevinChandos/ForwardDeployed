package io.bookworm.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Summary DTO of an authenticated member.
 * <p>
 * Why: Transports essential member identity and assigned security roles in tokens and auth responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSummaryDTO {
    private UUID memberId;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
}
