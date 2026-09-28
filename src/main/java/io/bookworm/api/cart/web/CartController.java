package io.bookworm.api.cart.web;

import io.bookworm.api.cart.application.CartService;
import io.bookworm.api.cart.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for shopping-cart operations: retrieval, item management,
 * quantity updates, removal, and guest-to-member merge.
 * <p>
 * Why: Supports dual guest/member identity by resolving the principal to either a
 * member UUID (from JWT) or the X-Guest-Token header, and delegates to {@link CartService}
 * for all cart domain logic.
 * Side effects: Item additions return 201 Created with a Location header per DESIGN-03.
 */
@Tag(name = "Cart", description = "Shopping cart — guest and authenticated")
@RestController
@RequestMapping("/cart")
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // ── GET /cart ─────────────────────────────────────────────────────────────

    @Operation(
            operationId = "getCart",
            summary = "Get current cart (guest or authenticated)"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart contents")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken) {

        UUID memberId = resolveOptionalId(principal);
        CartResponse response = cartService.getCart(memberId, guestToken, null);
        return ResponseEntity.ok(response);
    }

    // ── POST /cart/items ──────────────────────────────────────────────────────

    /**
     * DESIGN-03: adding a cart item creates a new CartItem resource → 201 with Location header.
     */
    @Operation(
            operationId = "addCartItem",
            summary = "Add item to cart"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added; returns updated cart"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Book format not found"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addCartItem(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody AddCartItemRequest request) {

        UUID memberId = resolveOptionalId(principal);
        CartResponse response = cartService.addItem(memberId, guestToken, null, request);

        // Why: the first CartItem in the updated cart corresponds to the newly added item;
        // in practice, the service should return the new item ID directly.
        UUID newItemId = response.getItems().stream()
                .filter(i -> i.getBookFormatId().equals(request.getBookFormatId()))
                .findFirst()
                .map(CartItemDTO::getCartItemId)
                .orElse(UUID.randomUUID());

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/cart/items/{id}").buildAndExpand(newItemId).toUri();

        return ResponseEntity.created(location).body(response);
    }

    // ── PATCH /cart/items/{cartItemId} ────────────────────────────────────────

    @Operation(
            operationId = "updateCartItem",
            summary = "Update cart item quantity"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Cart item not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        UUID memberId = resolveOptionalId(principal);
        CartResponse response = cartService.updateItemQuantity(memberId, guestToken, null, cartItemId, request);
        return ResponseEntity.ok(response);
    }

    // ── DELETE /cart/items/{cartItemId} ───────────────────────────────────────

    @Operation(
            operationId = "removeCartItem",
            summary = "Remove item from cart"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @PathVariable UUID cartItemId) {

        UUID memberId = resolveOptionalId(principal);
        cartService.removeItem(memberId, guestToken, cartItemId);
        return ResponseEntity.noContent().build();
    }

    // ── POST /cart/merge ──────────────────────────────────────────────────────

    @Operation(
            operationId = "mergeCart",
            summary = "Merge guest cart into member cart on login",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart merged"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MergeCartRequest request) {

        UUID memberId = resolveId(principal);
        CartResponse response = cartService.mergeCart(memberId, null, request);
        return ResponseEntity.ok(response);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Returns the authenticated member's UUID, or null for guest callers. */
    private UUID resolveOptionalId(UserDetails principal) {
        if (principal == null) return null;
        try {
            return UUID.fromString(principal.getUsername());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Returns the authenticated member's UUID (throws if unauthenticated). */
    private UUID resolveId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
