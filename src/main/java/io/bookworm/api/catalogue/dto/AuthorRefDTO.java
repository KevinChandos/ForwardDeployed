package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight author reference DTO.
 * <p>
 * Why: Provides minimal author identifier, display name, and role for book listings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRefDTO {
    private UUID authorId;
    private String name;
    private String role;
}
