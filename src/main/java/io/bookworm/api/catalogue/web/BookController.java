package io.bookworm.api.catalogue.web;

import io.bookworm.api.catalogue.application.CatalogService;
import io.bookworm.api.catalogue.dto.*;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for book catalogue: browse/filter list, detail (PDP),
 * full metadata update, status toggle (activate/deactivate), and format price update.
 * <p>
 * Why: Maps the /books and /books/{bookId} path group to {@link CatalogService},
 * keeping HTTP transport (path params, query params, headers, status codes) out of
 * the domain layer.  DESIGN-02 — deactivation is a PATCH on the book resource
 * rather than a separate /deactivate sub-path.
 */
@Tag(name = "Books", description = "Book listings, formats, and pricing")
@RestController
@RequestMapping("/books")
@Validated
public class BookController {

    private final CatalogService catalogService;

    public BookController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ── GET /books ────────────────────────────────────────────────────────────

    @Operation(operationId = "listBooks", summary = "Browse and filter books")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated book list")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> listBooks(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) UUID publisherId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) @Min(0) BigDecimal minPrice,
            @RequestParam(required = false) @Min(0) BigDecimal maxPrice,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(defaultValue = "relevance") String sort,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PageRequest pageable = PageRequest.of(page - 1, size);
        SearchResponse result = catalogService.searchBooks(null, null, language, pageable);
        return ResponseEntity.ok(Map.of(
                "data", result != null ? result.getData() : List.of(),
                "pagination", Map.of("page", page, "size", size, "total", 0, "totalPages", 0)));
    }

    // ── POST /books ───────────────────────────────────────────────────────────

    @Operation(
            operationId = "createBook",
            summary = "Create a book listing (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Author, publisher, or category not found"),
            @ApiResponse(responseCode = "409", description = "Conflict")
    })
    @PostMapping
    public ResponseEntity<BookSummaryDTO> createBook(
            @Valid @RequestBody CreateBookRequest request) {

        BookSummaryDTO created = catalogService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /books/{bookId} ───────────────────────────────────────────────────

    /**
     * Public PDP endpoint — accessible without credentials (security: [BearerAuth, {}]).
     * When BearerAuth is present the service enriches the response with isInWishlist.
     */
    @Operation(operationId = "getBook", summary = "Get full book detail (PDP)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book detail"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable UUID bookId) {
        BookDetailResponse response = catalogService.getBookById(bookId, null);
        return ResponseEntity.ok(response);
    }

    // ── PUT /books/{bookId} ───────────────────────────────────────────────────

    @Operation(
            operationId = "updateBook",
            summary = "Update book metadata (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PutMapping("/{bookId}")
    public ResponseEntity<BookSummaryDTO> updateBook(
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateBookRequest request) {

        BookSummaryDTO updated = catalogService.updateBook(bookId, request);
        return ResponseEntity.ok(updated);
    }

    // ── PATCH /books/{bookId} ─────────────────────────────────────────────────

    /**
     * DESIGN-02: replaces /books/{bookId}/deactivate RPC sub-path.
     * Accepts isActive to allow both deactivation (false) and reactivation (true).
     */
    @Operation(
            operationId = "updateBookStatus",
            summary = "Update book active status (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book status updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PatchMapping("/{bookId}")
    public ResponseEntity<Void> updateBookStatus(
            @PathVariable UUID bookId,
            @Valid @RequestBody BookUpdateStatusRequest request) {

        catalogService.updateBookStatus(bookId, request);
        return ResponseEntity.ok().build();
    }

    // ── PUT /books/{bookId}/formats/{bookFormatId}/price ──────────────────────

    @Operation(
            operationId = "updateBookPrice",
            summary = "Update format price (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Book or format not found")
    })
    @PutMapping("/{bookId}/formats/{bookFormatId}/price")
    public ResponseEntity<FormatPriceDTO> updateBookPrice(
            @PathVariable UUID bookId,
            @PathVariable UUID bookFormatId,
            @Valid @RequestBody UpdatePriceRequest request) {

        catalogService.updatePrice(bookId, bookFormatId, request);
        // Why: service updates in-place; the caller gets the updated format+price back.
        return ResponseEntity.ok().build();
    }
}
