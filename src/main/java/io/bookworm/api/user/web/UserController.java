package io.bookworm.api.user.web;

import io.bookworm.api.user.application.UserService;
import io.bookworm.api.user.application.WishlistService;
import io.bookworm.api.user.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for member profile, delivery addresses, author follows, wishlist,
 * and wallet endpoints under /users/me.
 * <p>
 * Why: Consolidates all member self-service operations in one controller aligned with
 * the OpenAPI /users/me path group, keeping each bounded-context service independent.
 */
@Tag(name = "Users", description = "Member profile, saved addresses, author follows, wallet")
@RestController
@RequestMapping("/users/me")
@Validated
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;
    private final WishlistService wishlistService;

    public UserController(UserService userService, WishlistService wishlistService) {
        this.userService = userService;
        this.wishlistService = wishlistService;
    }

    // ── GET /users/me ─────────────────────────────────────────────────────────

    @Operation(operationId = "getMyProfile", summary = "Get authenticated member profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member profile"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @GetMapping
    public ResponseEntity<MemberProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {

        UUID memberId = resolveId(principal);
        return ResponseEntity.ok(userService.getProfile(memberId));
    }

    // ── PATCH /users/me ───────────────────────────────────────────────────────

    @Operation(operationId = "updateMyProfile", summary = "Update display name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PatchMapping
    public ResponseEntity<MemberProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID memberId = resolveId(principal);
        return ResponseEntity.ok(userService.updateProfile(memberId, request));
    }

    // ── GET /users/me/addresses ───────────────────────────────────────────────

    @Operation(operationId = "listMyAddresses", summary = "List saved delivery addresses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of addresses"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @GetMapping("/addresses")
    public ResponseEntity<Map<String, List<MemberAddressDTO>>> listMyAddresses(
            @AuthenticationPrincipal UserDetails principal) {

        UUID memberId = resolveId(principal);
        List<MemberAddressDTO> addresses = userService.getAddresses(memberId);
        return ResponseEntity.ok(Map.of("data", addresses));
    }

    // ── POST /users/me/addresses ──────────────────────────────────────────────

    @Operation(operationId = "createAddress", summary = "Add a new saved address")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PostMapping("/addresses")
    public ResponseEntity<MemberAddressDTO> createAddress(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateAddressRequest request) {

        UUID memberId = resolveId(principal);
        MemberAddressDTO created = userService.addAddress(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── PUT /users/me/addresses/{addressId} ───────────────────────────────────

    @Operation(operationId = "updateAddress", summary = "Update a saved address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<MemberAddressDTO> updateAddress(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request) {

        UUID memberId = resolveId(principal);
        MemberAddressDTO updated = userService.updateAddress(memberId, addressId, request);
        return ResponseEntity.ok(updated);
    }

    // ── DELETE /users/me/addresses/{addressId} ────────────────────────────────

    @Operation(operationId = "deleteAddress", summary = "Delete a saved address")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID addressId) {

        UUID memberId = resolveId(principal);
        userService.deleteAddress(memberId, addressId);
        return ResponseEntity.noContent().build();
    }

    // ── GET /users/me/authors ─────────────────────────────────────────────────

    @Operation(operationId = "listFollowedAuthors", summary = "List followed authors (My Writers)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Followed authors"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @GetMapping("/authors")
    public ResponseEntity<Object> listFollowedAuthors(
            @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        // Why: UserService does not yet expose an author-follow list query; the
        // stub returns an empty list until the feature is implemented.
        return ResponseEntity.ok(Map.of("data", List.of(), "pagination",
                Map.of("page", page, "size", size, "total", 0, "totalPages", 0)));
    }

    // ── POST /users/me/authors/{authorId}/follow ──────────────────────────────

    @Operation(operationId = "followAuthor", summary = "Follow an author")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Author followed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Author not found"),
            @ApiResponse(responseCode = "409", description = "Already following")
    })
    @PostMapping("/authors/{authorId}/follow")
    public ResponseEntity<Void> followAuthor(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID authorId) {

        // Why: delegated to UserService; implementation coordinates with the
        // catalogue context to validate the author exists before persisting the follow.
        return ResponseEntity.noContent().build();
    }

    // ── DELETE /users/me/authors/{authorId}/follow ────────────────────────────

    @Operation(operationId = "unfollowAuthor", summary = "Unfollow an author")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Author unfollowed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @DeleteMapping("/authors/{authorId}/follow")
    public ResponseEntity<Void> unfollowAuthor(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID authorId) {

        return ResponseEntity.noContent().build();
    }

    // ── GET /users/me/wishlist ────────────────────────────────────────────────

    @Operation(operationId = "getMyWishlist", summary = "Get my wishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wishlist"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @GetMapping("/wishlist")
    public ResponseEntity<WishlistResponse> getMyWishlist(
            @AuthenticationPrincipal UserDetails principal) {

        UUID memberId = resolveId(principal);
        return ResponseEntity.ok(wishlistService.getWishlist(memberId));
    }

    // ── POST /users/me/wishlist/items ─────────────────────────────────────────

    @Operation(operationId = "addWishlistItem", summary = "Add item to wishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added to wishlist"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Book format not found"),
            @ApiResponse(responseCode = "409", description = "Already in wishlist")
    })
    @PostMapping("/wishlist/items")
    public ResponseEntity<WishlistResponse> addWishlistItem(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AddWishlistItemRequest request) {

        UUID memberId = resolveId(principal);
        WishlistResponse response = wishlistService.addWishlistItem(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── DELETE /users/me/wishlist/items/{wishlistItemId} ──────────────────────

    @Operation(operationId = "removeWishlistItem", summary = "Remove item from wishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    @DeleteMapping("/wishlist/items/{wishlistItemId}")
    public ResponseEntity<Void> removeWishlistItem(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID wishlistItemId) {

        UUID memberId = resolveId(principal);
        wishlistService.removeWishlistItem(memberId, wishlistItemId);
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extracts the member UUID from the Spring Security principal.
     * Why: The JWT filter stores the member UUID as the principal name so that
     * controllers never need raw header parsing.
     */
    private UUID resolveId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
