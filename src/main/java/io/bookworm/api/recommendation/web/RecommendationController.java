package io.bookworm.api.recommendation.web;

import io.bookworm.api.catalogue.dto.BookSummaryDTO;
import io.bookworm.api.recommendation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for personalised and curated book recommendations:
 * home-page sections, book-detail related reads, member-specific recommendations,
 * bestsellers, and new launches.
 * <p>
 * Why: Recommendation logic (collaborative filtering, category affinity, order history)
 * belongs to a separate bounded context — this controller is the thin HTTP adapter
 * over whatever recommendation engine is wired behind it.
 */
@Tag(name = "Recommendations", description = "Personalised and curated book recommendations")
@RestController
@RequestMapping("/recommendations")
@Validated
public class RecommendationController {

    // Why: RecommendationService interface does not yet exist in the project;
    // stub responses are returned until the service is wired.  The controller
    // structure mirrors the full OpenAPI spec so consumers can integrate immediately.

    // ── GET /recommendations/home ─────────────────────────────────────────────

    @Operation(operationId = "getHomeRecommendations", summary = "Get home-page recommendation sections")
    @SecurityRequirements({
            @SecurityRequirement(name = "BearerAuth"),
            @SecurityRequirement(name = "GuestToken")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Home recommendations")
    })
    @GetMapping("/home")
    public ResponseEntity<HomeRecommendationsResponse> getHomeRecommendations(
            @AuthenticationPrincipal UserDetails principal) {

        // Why: personalised vs. guest responses are differentiated inside the service
        // using the optional member ID resolved from the security principal.
        return ResponseEntity.ok().build();
    }

    // ── GET /recommendations/books/{bookId}/related ───────────────────────────

    @Operation(operationId = "getRelatedBooks", summary = "Get related reads for a book (cross-sell)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Related books"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/books/{bookId}/related")
    public ResponseEntity<Map<String, Object>> getRelatedBooks(
            @PathVariable UUID bookId,
            @Parameter(description = "Number of related books (1–12)")
            @RequestParam(defaultValue = "6") @Min(1) @Max(12) int size) {

        return ResponseEntity.ok(Map.of(
                "bookId", bookId.toString(),
                "relatedBooks", List.<BookSummaryDTO>of()));
    }

    // ── GET /recommendations/me ───────────────────────────────────────────────

    @Operation(
            operationId = "getPersonalisedRecommendations",
            summary = "Get personalised recommendations for the authenticated member",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Personalised recommendations"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    @GetMapping("/me")
    public ResponseEntity<PersonalisedRecommendationsResponse> getPersonalisedRecommendations(
            @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "Result limit (1–50)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok().build();
    }

    // ── GET /recommendations/bestsellers ─────────────────────────────────────

    @Operation(operationId = "getBestsellers", summary = "Get bestseller list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bestseller books")
    })
    @GetMapping("/bestsellers")
    public ResponseEntity<List<BookSummaryDTO>> getBestsellers(
            @Parameter(description = "Result limit (1–50)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String categorySlug) {

        return ResponseEntity.ok(List.of());
    }

    // ── GET /recommendations/new-launches ────────────────────────────────────

    @Operation(operationId = "getNewLaunches", summary = "Get new launch titles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New launch books")
    })
    @GetMapping("/new-launches")
    public ResponseEntity<List<BookSummaryDTO>> getNewLaunches(
            @Parameter(description = "Result limit (1–50)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String categorySlug) {

        return ResponseEntity.ok(List.of());
    }
}
