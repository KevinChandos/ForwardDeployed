package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Detailed author reference DTO.
 * <p>
 * Why: Provides author identity, name, role, photo URL, and biography for author lists on book detail pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDetailRefDTO {
    private UUID authorId;
    private String name;
    private String role;
    private String photoUrl;
    private String bio;
}
