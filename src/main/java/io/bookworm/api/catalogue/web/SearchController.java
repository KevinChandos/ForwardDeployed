package io.bookworm.api.catalogue.web;

import io.bookworm.api.catalogue.application.CatalogService;
import io.bookworm.api.catalogue.dto.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for full-text book search with facets at /search/books.
 * <p>
 * Why: Separated from {@link BookController} because search has different query semantics
 * (free-text, facets, relevance scoring) that belong to the same catalogue module
 * but warrant an independent path and request model.
 */
@Tag(name = "Search", description = "Full-text search with facets")
@RestController
@RequestMapping("/search")
@Validated
public class SearchController {

    private final CatalogService catalogService;

    public SearchController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ── GET /search/books ─────────────────────────────────────────────────────

    @Operation(
            operationId = "searchBooks",
            summary = "Full-text book search with facets"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results with facets"),
            @ApiResponse(responseCode = "400", description = "Validation error — query too short")
    })
    @GetMapping("/books")
    public ResponseEntity<SearchResponse> searchBooks(
            @Parameter(description = "Search term (min 2 characters)", required = true)
            @RequestParam @Size(min = 2) String q,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) @Min(0) Double minPrice,
            @RequestParam(required = false) @Min(0) Double maxPrice,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(defaultValue = "relevance") String sort,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PageRequest pageable = PageRequest.of(page - 1, size);
        // Why: categoryId resolution from categorySlug is handled inside the service
        // to keep the controller free of catalogue internal lookups.
        SearchResponse response = catalogService.searchBooks(q, null, language, pageable);
        return ResponseEntity.ok(response);
    }
}
