package io.bookworm.api.catalogue.application;

import io.bookworm.api.catalogue.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for catalogue operations: books, authors, publishers, and categories.
 * <p>
 * Why: Encapsulates business logic, inventory validations, catalog hierarchy traversal,
 * pricing lookup, and CRUD operations across the catalogue bounded context.
 */
public interface CatalogService {

    // ── Book Operations ────────────────────────────────────────────────────────
    BookDetailResponse getBookById(UUID bookId, UUID storeId);

    BookSummaryDTO createBook(CreateBookRequest request);

    BookSummaryDTO updateBook(UUID bookId, UpdateBookRequest request);

    void updateBookStatus(UUID bookId, BookUpdateStatusRequest request);

    void updatePrice(UUID bookId, UUID formatId, UpdatePriceRequest request);

    SearchResponse searchBooks(String query, UUID categoryId, String language, Pageable pageable);

    // ── Author Operations ──────────────────────────────────────────────────────
    AuthorDetailResponse getAuthorById(UUID authorId, Pageable pageable);

    AuthorSummaryDTO createAuthor(CreateAuthorRequest request);

    AuthorSummaryDTO updateAuthor(UUID authorId, UpdateAuthorRequest request);

    // ── Publisher Operations ───────────────────────────────────────────────────
    PublisherDetailResponse getPublisherById(UUID publisherId, Pageable pageable);

    PublisherDTO createPublisher(CreatePublisherRequest request);

    PublisherDTO updatePublisher(UUID publisherId, UpdatePublisherRequest request);

    // ── Category Operations ────────────────────────────────────────────────────
    CategoryTreeResponse getCategoryTree();

    CategoryRefDTO getCategoryBySlug(String slug);

    CategoryRefDTO createCategory(CreateCategoryRequest request);

    CategoryRefDTO updateCategory(UUID categoryId, UpdateCategoryRequest request);
}
