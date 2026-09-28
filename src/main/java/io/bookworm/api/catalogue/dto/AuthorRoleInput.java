package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Author assignment with role specification for book creation.
 * <p>
 * Why: Associates author ID with their designated role on the book (e.g. AUTHOR, EDITOR, TRANSLATOR).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRoleInput {

    @NotNull(message = "Author ID is required")
    private UUID authorId;

    @NotNull(message = "Role is required")
    private String role;
}
