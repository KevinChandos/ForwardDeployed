package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for toggling a book's active status.
 * <p>
 * Why: Enables catalogue managers to activate or deactivate a book in the public catalogue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdateStatusRequest {

    @NotNull(message = "isActive flag is required")
    private Boolean isActive;
}
