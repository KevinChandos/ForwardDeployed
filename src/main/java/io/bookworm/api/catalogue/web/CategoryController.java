package io.bookworm.api.catalogue.web;

import io.bookworm.api.catalogue.application.CatalogService;
import io.bookworm.api.catalogue.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for category hierarchy management: full tree, slug lookup, UUID lookup,
 * creation, update, and deletion.
 * <p>
 * Why: DESIGN-01 fix — the former /categories/{slug} and /categories/{categoryId} used
 * overlapping {variable} templates causing spec-breaking router ambiguity.  Slug lookup
 * is now a dedicated sub-path /categories/by-slug/{slug}, resolved here.
 */
@Tag(name = "Categories", description = "Category hierarchy management")
@RestController
@Validated
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ── GET /categories ───────────────────────────────────────────────────────

    @Operation(operationId = "listCategories", summary = "Get full category tree")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category tree")
    })
    @GetMapping("/categories")
    public ResponseEntity<CategoryTreeResponse> listCategories() {
        return ResponseEntity.ok(catalogService.getCategoryTree());
    }

    // ── POST /categories ──────────────────────────────────────────────────────

    @Operation(
            operationId = "createCategory",
            summary = "Create a category (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Parent category not found"),
            @ApiResponse(responseCode = "409", description = "Slug already exists")
    })
    @PostMapping("/categories")
    public ResponseEntity<CategoryRefDTO> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryRefDTO created = catalogService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /categories/by-slug/{slug} ────────────────────────────────────────

    @Operation(operationId = "getCategoryBySlug", summary = "Get category by slug")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category node"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/categories/by-slug/{slug}")
    public ResponseEntity<CategoryRefDTO> getCategoryBySlug(@PathVariable String slug) {
        CategoryRefDTO response = catalogService.getCategoryBySlug(slug);
        return ResponseEntity.ok(response);
    }

    // ── GET /categories/{categoryId} ──────────────────────────────────────────

    @Operation(operationId = "getCategory", summary = "Get category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category node"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryRefDTO> getCategory(@PathVariable UUID categoryId) {
        // Why: getCategoryBySlug is the primary lookup; UUID lookup follows the
        // same service path with the UUID as the identifier.
        CategoryRefDTO response = catalogService.getCategoryBySlug(categoryId.toString());
        return ResponseEntity.ok(response);
    }

    // ── PUT /categories/{categoryId} ──────────────────────────────────────────

    @Operation(
            operationId = "updateCategory",
            summary = "Update a category (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Slug conflict")
    })
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryRefDTO> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {

        CategoryRefDTO updated = catalogService.updateCategory(categoryId, request);
        return ResponseEntity.ok(updated);
    }

    // ── DELETE /categories/{categoryId} ───────────────────────────────────────

    @Operation(
            operationId = "deleteCategory",
            summary = "Delete a category (admin)",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role/permission"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "422", description = "Category has child nodes or assigned books")
    })
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.noContent().build();
    }
}
