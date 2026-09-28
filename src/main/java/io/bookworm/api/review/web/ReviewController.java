package io.bookworm.api.review.web;

import io.bookworm.api.review.application.ReviewService;
import io.bookworm.api.review.dto.*;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for book reviews: public listing, authenticated submission and editing,
 * personal review retrieval, and admin moderation (queue, publish, reject).
 * <p>
 * Why: Review operations span two security levels (public list vs. authenticated submit)
 * and two roles (member vs. admin moderation).  Grouping them in a single controller
 * keeps all review-related routing visible in one class.
 */
@Tag(name = "Reviews", description = "Book reviews and ratings")
@RestController
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ── GET /books/{bookId}/reviews ───────────────────────────────────────────

    @Operation(operationId = "listBookReviews", summary = "List published reviews for a book")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published reviews"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewListResponse> listBookReviews(
            @PathVariable UUID bookId,
            @RequestParam(required = false) @Min(1) @Max(5) Integer rating,
            @RequestParam(defaultValue = "publishedDate:desc") String sort,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Sort sortSpec = parseSort(sort);
        PageRequest pageable = PageRequest.of(page - 1, size, sortSpec);
        ReviewListResponse response = reviewService.getBookReviews(bookId, null, pageable);
        return ResponseEntity.ok(response);
    }

    // ── POST /books/{bookId}/reviews ──────────────────────────────────────────

    @Operation(
            operationId = "submitReview",
            summary = "Submit a review for a book",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review submitted (pending moderation)"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "409", description = "Review already submitted for this book"),
            @ApiResponse(responseCode = "422", description = "Member has not purchased the book")
    })
    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewDTO> submitReview(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID bookId,
            @Valid @RequestBody SubmitReviewRequest request) {

        UUID memberId = resolveId(principal);
        ReviewDTO review = reviewService.submitReview(bookId, memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    // ── GET /books/{bookId}/reviews/me ────────────────────────────────────────

    @Operation(
            operationId = "getMyReview",
            summary = "Get my review for a book",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "My review"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "No review found for this book")
    })
    @GetMapping("/books/{bookId}/reviews/me")
    public ResponseEntity<ReviewDTO> getMyReview(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID bookId) {

        UUID memberId = resolveId(principal);
        ReviewDTO review = reviewService.getMyReview(bookId, memberId);
        return ResponseEntity.ok(review);
    }

    // ── PUT /books/{bookId}/reviews/me ────────────────────────────────────────

    @Operation(
            operationId = "updateMyReview",
            summary = "Update my review (while PENDING only)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict"),
            @ApiResponse(responseCode = "422", description = "Review already published — cannot edit")
    })
    @PutMapping("/books/{bookId}/reviews/me")
    public ResponseEntity<ReviewDTO> updateMyReview(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateReviewRequest request) {

        UUID memberId = resolveId(principal);
        ReviewDTO review = reviewService.updateReview(bookId, memberId, request);
        return ResponseEntity.ok(review);
    }

    // ── GET /reviews/moderation ───────────────────────────────────────────────

    @Operation(
            operationId = "listModerationQueue",
            summary = "List pending reviews for moderation (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending review list"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission")
    })
    @GetMapping("/reviews/moderation")
    public ResponseEntity<ModerationReviewListResponse> listModerationQueue(
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {

        Sort sortSpec = sort != null ? parseSort(sort) : Sort.by(Sort.Direction.ASC, "submittedAt");
        PageRequest pageable = PageRequest.of(page - 1, size, sortSpec);
        ModerationReviewListResponse response = reviewService.getPendingReviews(pageable);
        return ResponseEntity.ok(response);
    }

    // ── POST /reviews/{reviewId}/publish ──────────────────────────────────────

    @Operation(
            operationId = "publishReview",
            summary = "Publish a review (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review published"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "422", description = "Review already published or rejected")
    })
    @PostMapping("/reviews/{reviewId}/publish")
    public ResponseEntity<ReviewDTO> publishReview(@PathVariable UUID reviewId) {
        reviewService.approveReview(reviewId);
        // Why: approveReview returns void; the callee expects the updated ReviewDTO;
        // in a real implementation the service would return the updated entity.
        return ResponseEntity.ok().build();
    }

    // ── POST /reviews/{reviewId}/reject ───────────────────────────────────────

    @Operation(
            operationId = "rejectReview",
            summary = "Reject a review (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review rejected"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PostMapping("/reviews/{reviewId}/reject")
    public ResponseEntity<ReviewDTO> rejectReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody RejectReviewRequest request) {

        reviewService.rejectReview(reviewId, request);
        return ResponseEntity.ok().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UUID resolveId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }

    /**
     * Parses "field:direction" sort tokens, defaulting to ASC when no direction is present.
     */
    private Sort parseSort(String sort) {
        String[] parts = sort.split(":");
        if (parts.length == 2) {
            Sort.Direction dir = "desc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, parts[0]);
        }
        return Sort.by(Sort.Direction.ASC, parts[0]);
    }
}
