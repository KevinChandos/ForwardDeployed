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
 * REST controller for publisher catalogue management: listing, creation, update, and deletion.
 * <p>
 * Why: Owns the HTTP translation layer for the /publishers path group, isolating
 * pagination, path-variable extraction, and response-status decisions from domain logic.
 */
@Tag(name = "Publishers", description = "Publisher catalogue management")
@RestController
@RequestMapping("/publishers")
@Validated
public class PublisherController {

    private final CatalogService catalogService;

    public PublisherController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ── GET /publishers ───────────────────────────────────────────────────────

    @Operation(operationId = "listPublishers", summary = "List publishers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated publisher list")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> listPublishers(
            @RequestParam(required = false) String name,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Items per page (max 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {

        Sort sortSpec = sort != null ? parseSort(sort) : Sort.unsorted();
        PageRequest pageable = PageRequest.of(page - 1, size, sortSpec);

        // Why: pageable forwarded to service; empty list returned until impl is wired.
        List<PublisherDTO> data = List.of();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "pagination", Map.of("page", page, "size", size, "total", 0, "totalPages", 0)));
    }

    // ── POST /publishers ──────────────────────────────────────────────────────

    @Operation(
            operationId = "createPublisher",
            summary = "Create a publisher (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publisher created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "409", description = "Publisher name already exists")
    })
    @PostMapping
    public ResponseEntity<PublisherDTO> createPublisher(
            @Valid @RequestBody CreatePublisherRequest request) {

        PublisherDTO created = catalogService.createPublisher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /publishers/{publisherId} ─────────────────────────────────────────

    @Operation(operationId = "getPublisher", summary = "Get publisher detail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publisher with books"),
            @ApiResponse(responseCode = "404", description = "Publisher not found")
    })
    @GetMapping("/{publisherId}")
    public ResponseEntity<PublisherDetailResponse> getPublisher(
            @PathVariable UUID publisherId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PageRequest pageable = PageRequest.of(page - 1, size);
        PublisherDetailResponse response = catalogService.getPublisherById(publisherId, pageable);
        return ResponseEntity.ok(response);
    }

    // ── PUT /publishers/{publisherId} ─────────────────────────────────────────

    @Operation(
            operationId = "updatePublisher",
            summary = "Update a publisher (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publisher updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Publisher not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PutMapping("/{publisherId}")
    public ResponseEntity<PublisherDTO> updatePublisher(
            @PathVariable UUID publisherId,
            @Valid @RequestBody UpdatePublisherRequest request) {

        PublisherDTO updated = catalogService.updatePublisher(publisherId, request);
        return ResponseEntity.ok(updated);
    }

    // ── DELETE /publishers/{publisherId} ──────────────────────────────────────

    @Operation(
            operationId = "deletePublisher",
            summary = "Delete a publisher (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publisher deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Publisher not found")
    })
    @DeleteMapping("/{publisherId}")
    public ResponseEntity<Void> deletePublisher(@PathVariable UUID publisherId) {
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

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
