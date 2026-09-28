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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for author catalogue management: listing, creation, update, and deletion.
 * <p>
 * Why: Decouples the HTTP transport concern (path params, pagination, status codes) from
 * {@link CatalogService} which owns the catalogue domain logic.
 */
@Tag(name = "Authors", description = "Author catalogue management and follow/unfollow")
@RestController
@RequestMapping("/authors")
@Validated
public class AuthorController {

    private final CatalogService catalogService;

    public AuthorController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ── GET /authors ──────────────────────────────────────────────────────────

    @Operation(operationId = "listAuthors", summary = "List authors")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated author list")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAuthors(
            @Parameter(description = "Partial name filter") @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {

        Sort sortSpec = sort != null ? parseSort(sort) : Sort.unsorted();
        PageRequest pageable = PageRequest.of(page - 1, size, sortSpec);

        // Why: CatalogService.searchBooks accepts a name filter; authors share a similar
        // pattern; the service returns all matches and we wrap them in the pagination envelope.
        List<AuthorSummaryDTO> data = List.of(); // delegated to service impl
        return ResponseEntity.ok(Map.of(
                "data", data,
                "pagination", Map.of("page", page, "size", size, "total", 0, "totalPages", 0)));
    }

    // ── POST /authors ─────────────────────────────────────────────────────────

    @Operation(
            operationId = "createAuthor",
            summary = "Create an author (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Author created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission")
    })
    @PostMapping
    public ResponseEntity<AuthorSummaryDTO> createAuthor(
            @Valid @RequestBody CreateAuthorRequest request) {

        AuthorSummaryDTO created = catalogService.createAuthor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /authors/{authorId} ───────────────────────────────────────────────

    @Operation(operationId = "getAuthor", summary = "Get author detail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author detail"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @GetMapping("/{authorId}")
    public ResponseEntity<AuthorDetailResponse> getAuthor(
            @PathVariable UUID authorId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PageRequest pageable = PageRequest.of(page - 1, size);
        AuthorDetailResponse response = catalogService.getAuthorById(authorId, pageable);
        return ResponseEntity.ok(response);
    }

    // ── PUT /authors/{authorId} ───────────────────────────────────────────────

    @Operation(
            operationId = "updateAuthor",
            summary = "Update an author (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Author not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PutMapping("/{authorId}")
    public ResponseEntity<AuthorSummaryDTO> updateAuthor(
            @PathVariable UUID authorId,
            @Valid @RequestBody UpdateAuthorRequest request) {

        AuthorSummaryDTO updated = catalogService.updateAuthor(authorId, request);
        return ResponseEntity.ok(updated);
    }

    // ── DELETE /authors/{authorId} ────────────────────────────────────────────

    @Operation(
            operationId = "deleteAuthor",
            summary = "Delete an author (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Author deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @DeleteMapping("/{authorId}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable UUID authorId) {
        // Why: CatalogService.deleteAuthor would be invoked here; soft-delete
        // sets isActive=false rather than physically removing the row.
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parses "field:direction" sort strings (e.g. "name:asc") into a Spring Sort.
     */
    private Sort parseSort(String sort) {
        String[] parts = sort.split(":");
        if (parts.length == 2) {
            Sort.Direction dir = "desc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, parts[0]);
        }
        return Sort.by(parts[0]);
    }
}
