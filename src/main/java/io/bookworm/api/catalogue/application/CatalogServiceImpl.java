package io.bookworm.api.catalogue.application;

import io.bookworm.api.catalogue.domain.*;
import io.bookworm.api.catalogue.dto.*;
import io.bookworm.api.catalogue.infrastructure.*;
import io.bookworm.api.catalogue.mapper.CatalogueMapper;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.dto.PaginationDTO;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.store.domain.Store;
import io.bookworm.api.store.infrastructure.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service implementation for managing catalogue items including Books, Authors, Publishers, and Categories.
 * <p>
 * Why: Central orchestrator for book lifecycle management, metadata association, catalog browsing,
 * hierarchy resolution, and pricing effective period management.
 * Side effects: Mutates books, formats, prices, authors, publishers, and categories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final BookFormatRepository bookFormatRepository;
    private final BookPriceRepository bookPriceRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final StoreRepository storeRepository;
    private final CatalogueMapper catalogueMapper;

    // ── Book Operations ────────────────────────────────────────────────────────

    @Override
    public BookDetailResponse getBookById(UUID bookId, UUID storeId) {
        log.info("Fetching book details for bookId: {}, storeId: {}", bookId, storeId);
        Book book = bookRepository.findActiveWithDetails(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        List<AuthorDetailRefDTO> authors = book.getBookAuthors() != null ?
                book.getBookAuthors().stream()
                        .filter(ba -> ba.getDeletedAt() == null && ba.getAuthor() != null)
                        .map(catalogueMapper::toAuthorDetailRefDTO)
                        .toList() : Collections.emptyList();

        List<CategoryRefDTO> categories = book.getBookCategories() != null ?
                book.getBookCategories().stream()
                        .filter(bc -> bc.getDeletedAt() == null && bc.getCategory() != null)
                        .map(bc -> catalogueMapper::toCategoryRefDTO != null ? catalogueMapper.toCategoryRefDTO(bc.getCategory()) : null)
                        .filter(Objects::nonNull)
                        .toList() : Collections.emptyList();

        OffsetDateTime now = OffsetDateTime.now();
        List<FormatPriceDTO> formats = new ArrayList<>();
        BigDecimal lowestPrice = null;

        if (book.getFormats() != null) {
            for (BookFormat format : book.getFormats()) {
                if (format.getDeletedAt() == null && format.isActive()) {
                    BookPrice currentPrice = null;
                    if (storeId != null) {
                        currentPrice = bookPriceRepository.findCurrentPrice(format.getBookFormatId(), storeId, now).orElse(null);
                    }
                    if (currentPrice != null && (lowestPrice == null || currentPrice.getAmount().compareTo(lowestPrice) < 0)) {
                        lowestPrice = currentPrice.getAmount();
                    }
                    formats.add(catalogueMapper.toFormatPriceDTO(format, currentPrice));
                }
            }
        }

        MoneyDTO startingPrice = lowestPrice != null ?
                MoneyDTO.builder().amount(lowestPrice.toPlainString()).currency("INR").build() : null;

        ReviewSummaryDTO reviews = ReviewSummaryDTO.builder()
                .averageRating(book.getAverageRating() != null ? book.getAverageRating() : 0.0)
                .totalReviews(book.getReviewCount() != null ? book.getReviewCount().longValue() : 0L)
                .ratingDistribution(RatingDistributionDTO.builder().star1(0).star2(0).star3(0).star4(0).star5(0).build())
                .build();

        return catalogueMapper.toBookDetailResponse(book, authors, categories, formats, startingPrice, reviews, null);
    }

    @Override
    @Transactional
    public BookSummaryDTO createBook(CreateBookRequest request) {
        log.info("Creating new book with title: {}", request.getTitle());

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessRuleException("INVALID_BOOK", "Book title is required");
        }

        Publisher publisher = null;
        if (request.getPublisherId() != null) {
            publisher = publisherRepository.findActive(request.getPublisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher", request.getPublisherId()));
        }

        Book book = new Book();
        book.setTitle(request.getTitle().trim());
        book.setSynopsis(request.getSynopsis());
        book.setLanguage(request.getLanguage() != null ? request.getLanguage() : "ENG");
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPublishedDate(request.getPublishedDate());
        book.setPublisher(publisher);
        book.setActive(true);

        Book savedBook = bookRepository.save(book);

        // Associate authors
        if (request.getAuthors() != null) {
            for (AuthorRoleInput input : request.getAuthors()) {
                Author author = authorRepository.findActive(input.getAuthorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Author", input.getAuthorId()));
                BookAuthor bookAuthor = new BookAuthor();
                bookAuthor.setBook(savedBook);
                bookAuthor.setAuthor(author);
                bookAuthor.setRole(input.getRole() != null ? BookAuthor.AuthorRole.valueOf(input.getRole()) : BookAuthor.AuthorRole.AUTHOR);
                bookAuthorRepository.save(bookAuthor);
            }
        }

        // Associate categories
        if (request.getCategoryIds() != null) {
            for (UUID catId : request.getCategoryIds()) {
                Category cat = categoryRepository.findActive(catId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category", catId));
                BookCategory bc = new BookCategory();
                bc.setBook(savedBook);
                bc.setCategory(cat);
                bookCategoryRepository.save(bc);
            }
        }

        // Formats & initial prices
        List<FormatPriceDTO> formatDTOs = new ArrayList<>();
        if (request.getFormats() != null) {
            for (CreateFormatDTO fDto : request.getFormats()) {
                BookFormat format = new BookFormat();
                format.setBook(savedBook);
                format.setFormatType(BookFormat.FormatType.valueOf(fDto.getFormatType()));
                format.setIsbn(fDto.getIsbn());
                format.setPageCount(fDto.getPageCount());
                format.setWeightGrams(fDto.getWeightGrams());
                format.setActive(true);
                BookFormat savedFormat = bookFormatRepository.save(format);

                BookPrice savedPrice = null;
                if (fDto.getPrice() != null) {
                    Store store = storeRepository.findActiveDefaultStore()
                            .orElseGet(() -> {
                                List<Store> all = storeRepository.findAll();
                                return all.isEmpty() ? null : all.get(0);
                            });
                    if (store != null) {
                        BookPrice price = new BookPrice();
                        price.setBookFormat(savedFormat);
                        price.setStore(store);
                        price.setAmount(new BigDecimal(fDto.getPrice().getAmount()));
                        price.setCurrency(fDto.getPrice().getCurrency() != null ? fDto.getPrice().getCurrency() : "INR");
                        price.setEffectiveFrom(OffsetDateTime.now());
                        savedPrice = bookPriceRepository.save(price);
                    }
                }
                formatDTOs.add(catalogueMapper.toFormatPriceDTO(savedFormat, savedPrice));
            }
        }

        log.info("Book created successfully with ID: {}", savedBook.getBookId());
        return catalogueMapper.toBookSummaryDTO(savedBook, formatDTOs, null);
    }

    @Override
    @Transactional
    public BookSummaryDTO updateBook(UUID bookId, UpdateBookRequest request) {
        log.info("Updating book with ID: {}", bookId);
        Book book = bookRepository.findActiveWithDetails(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        if (request.getVersion() != null && !request.getVersion().equals(book.getVersion())) {
            throw new BusinessRuleException("OPTIMISTIC_LOCK_CONFLICT", "The book was modified by another transaction");
        }

        if (request.getTitle() != null) book.setTitle(request.getTitle());
        if (request.getSynopsis() != null) book.setSynopsis(request.getSynopsis());
        if (request.getLanguage() != null) book.setLanguage(request.getLanguage());
        if (request.getCoverImageUrl() != null) book.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getPublishedDate() != null) book.setPublishedDate(request.getPublishedDate());
        if (request.getPublisherId() != null) {
            Publisher pub = publisherRepository.findActive(request.getPublisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher", request.getPublisherId()));
            book.setPublisher(pub);
        }

        Book saved = bookRepository.save(book);
        return catalogueMapper.toSimpleBookSummaryDTO(saved);
    }

    @Override
    @Transactional
    public void updateBookStatus(UUID bookId, BookUpdateStatusRequest request) {
        log.info("Updating book status for bookId: {} to active={}", bookId, request.getIsActive());
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        book.setActive(Boolean.TRUE.equals(request.getIsActive()));
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public void updatePrice(UUID bookId, UUID formatId, UpdatePriceRequest request) {
        log.info("Updating price for bookId: {}, formatId: {}", bookId, formatId);
        BookFormat format = bookFormatRepository.findActiveByIdAndBookId(formatId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("BookFormat", formatId));

        OffsetDateTime now = OffsetDateTime.now();
        Store store = storeRepository.findActiveDefaultStore()
                .orElseGet(() -> {
                    List<Store> all = storeRepository.findAll();
                    return all.isEmpty() ? null : all.get(0);
                });

        if (store == null) {
            throw new BusinessRuleException("STORE_NOT_FOUND", "No active default store configured");
        }

        // Close out previous price
        bookPriceRepository.findCurrentPrice(formatId, store.getStoreId(), now).ifPresent(p -> {
            p.setEffectiveTo(now);
            bookPriceRepository.save(p);
        });

        // Insert new price
        BookPrice newPrice = new BookPrice();
        newPrice.setBookFormat(format);
        newPrice.setStore(store);
        newPrice.setAmount(new BigDecimal(request.getPrice().getAmount()));
        newPrice.setCurrency(request.getPrice().getCurrency() != null ? request.getPrice().getCurrency() : "INR");
        newPrice.setEffectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : now);
        newPrice.setEffectiveTo(request.getEffectiveTo());
        bookPriceRepository.save(newPrice);
    }

    @Override
    public SearchResponse searchBooks(String query, UUID categoryId, String language, Pageable pageable) {
        log.info("Searching books query='{}', categoryId={}, language={}", query, categoryId, language);

        Specification<Book> spec = (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isTrue(root.get("isActive")));

            if (query != null && !query.isBlank()) {
                String likePattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), likePattern));
            }
            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(root.get("language"), language.trim()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Book> page = bookRepository.findAll(spec, pageable);
        List<BookSummaryDTO> content = page.getContent().stream()
                .map(catalogueMapper::toSimpleBookSummaryDTO)
                .toList();

        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return SearchResponse.builder()
                .content(content)
                .pagination(pagination)
                .build();
    }

    // ── Author Operations ──────────────────────────────────────────────────────

    @Override
    public AuthorDetailResponse getAuthorById(UUID authorId, Pageable pageable) {
        log.info("Fetching author detail for authorId: {}", authorId);
        Author author = authorRepository.findActive(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", authorId));

        AuthorDetailResponse response = catalogueMapper.toAuthorDetailResponse(author);
        response.setFollowerCount(authorRepository.countActiveFollowers(authorId));
        return response;
    }

    @Override
    @Transactional
    public AuthorSummaryDTO createAuthor(CreateAuthorRequest request) {
        log.info("Creating author with name: {}", request.getName());
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessRuleException("INVALID_AUTHOR", "Author name is required");
        }
        Author author = catalogueMapper.toAuthorEntity(request);
        Author saved = authorRepository.save(author);
        return catalogueMapper.toAuthorSummaryDTO(saved);
    }

    @Override
    @Transactional
    public AuthorSummaryDTO updateAuthor(UUID authorId, UpdateAuthorRequest request) {
        log.info("Updating author with ID: {}", authorId);
        Author author = authorRepository.findActive(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", authorId));

        if (request.getVersion() != null && !request.getVersion().equals(author.getVersion())) {
            throw new BusinessRuleException("OPTIMISTIC_LOCK_CONFLICT", "The author was modified by another transaction");
        }

        catalogueMapper.updateAuthorEntityFromDTO(request, author);
        Author saved = authorRepository.save(author);
        return catalogueMapper.toAuthorSummaryDTO(saved);
    }

    // ── Publisher Operations ───────────────────────────────────────────────────

    @Override
    public PublisherDetailResponse getPublisherById(UUID publisherId, Pageable pageable) {
        log.info("Fetching publisher details for ID: {}", publisherId);
        Publisher publisher = publisherRepository.findActive(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", publisherId));
        return catalogueMapper.toPublisherDetailResponse(publisher);
    }

    @Override
    @Transactional
    public PublisherDTO createPublisher(CreatePublisherRequest request) {
        log.info("Creating publisher with name: {}", request.getName());
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessRuleException("INVALID_PUBLISHER", "Publisher name is required");
        }
        if (publisherRepository.existsActiveByName(request.getName().trim())) {
            throw new BusinessRuleException("DUPLICATE_PUBLISHER", "Publisher name already exists");
        }
        Publisher publisher = catalogueMapper.toPublisherEntity(request);
        Publisher saved = publisherRepository.save(publisher);
        return catalogueMapper.toPublisherDTO(saved);
    }

    @Override
    @Transactional
    public PublisherDTO updatePublisher(UUID publisherId, UpdatePublisherRequest request) {
        log.info("Updating publisher with ID: {}", publisherId);
        Publisher publisher = publisherRepository.findActive(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", publisherId));

        if (request.getVersion() != null && !request.getVersion().equals(publisher.getVersion())) {
            throw new BusinessRuleException("OPTIMISTIC_LOCK_CONFLICT", "The publisher was modified by another transaction");
        }

        catalogueMapper.updatePublisherEntityFromDTO(request, publisher);
        Publisher saved = publisherRepository.save(publisher);
        return catalogueMapper.toPublisherDTO(saved);
    }

    // ── Category Operations ────────────────────────────────────────────────────

    @Override
    public CategoryTreeResponse getCategoryTree() {
        log.info("Fetching complete active category tree");
        List<Category> roots = categoryRepository.findActiveRoots();
        List<CategoryNodeDTO> rootNodes = roots.stream()
                .map(this::buildCategoryHierarchy)
                .toList();

        return CategoryTreeResponse.builder()
                .categories(rootNodes)
                .build();
    }

    private CategoryNodeDTO buildCategoryHierarchy(Category category) {
        CategoryNodeDTO node = catalogueMapper.toCategoryNodeDTO(category);
        List<Category> children = categoryRepository.findActiveByParentId(category.getCategoryId());
        if (children != null && !children.isEmpty()) {
            node.setChildren(children.stream().map(this::buildCategoryHierarchy).toList());
        }
        return node;
    }

    @Override
    public CategoryRefDTO getCategoryBySlug(String slug) {
        log.info("Fetching category by slug: {}", slug);
        Category cat = categoryRepository.findActiveBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category slug", slug));
        return catalogueMapper.toCategoryRefDTO(cat);
    }

    @Override
    @Transactional
    public CategoryRefDTO createCategory(CreateCategoryRequest request) {
        log.info("Creating category with name: {}", request.getName());
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessRuleException("INVALID_CATEGORY", "Category name is required");
        }
        Category category = catalogueMapper.toCategoryEntity(request);
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findActive(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", request.getParentId()));
            category.setParent(parent);
        }
        Category saved = categoryRepository.save(category);
        return catalogueMapper.toCategoryRefDTO(saved);
    }

    @Override
    @Transactional
    public CategoryRefDTO updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        log.info("Updating category with ID: {}", categoryId);
        Category category = categoryRepository.findActive(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        if (request.getVersion() != null && !request.getVersion().equals(category.getVersion())) {
            throw new BusinessRuleException("OPTIMISTIC_LOCK_CONFLICT", "The category was modified by another transaction");
        }

        catalogueMapper.updateCategoryEntityFromDTO(request, category);
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findActive(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", request.getParentId()));
            category.setParent(parent);
        }
        Category saved = categoryRepository.save(category);
        return catalogueMapper.toCategoryRefDTO(saved);
    }
}
