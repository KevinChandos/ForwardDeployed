package io.bookworm.api.cart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for merging an anonymous guest cart into an authenticated member cart.
 * <p>
 * Why: Accepts the guest session token whose items will be combined into the member's cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest {

    @NotBlank(message = "Guest token is required")
    private String guestToken;
}
