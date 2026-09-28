package io.bookworm.api.catalogue.mapper;

import io.bookworm.api.catalogue.domain.*;
import io.bookworm.api.catalogue.dto.*;
import io.bookworm.api.common.domain.Money;
import io.bookworm.api.common.dto.MoneyDTO;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * MapStruct mapper for Catalogue bounded context (Authors, Publishers, Categories, Books, Formats, Prices).
 * <p>
 * Why: Converts domain entities to REST DTOs with null safety and custom mappings for pricing and hierarchy.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CatalogueMapper {

    // ── Money Mapping ──────────────────────────────────────────────────────────
    default MoneyDTO toMoneyDTO(Money money) {
        if (money == null || money.getAmount() == null) {
            return null;
        }
        return MoneyDTO.builder()
                .amount(money.getAmount().toPlainString())
                .currency(money.getCurrency() != null ? money.getCurrency() : "INR")
                .build();
    }

    default Money toMoney(MoneyDTO dto) {
        if (dto == null || dto.getAmount() == null) {
            return null;
        }
        return new Money(new BigDecimal(dto.getAmount()), dto.getCurrency() != null ? dto.getCurrency() : "INR");
    }

    // ── Author Mappings ───────────────────────────────────────────────────────
    AuthorSummaryDTO toAuthorSummaryDTO(Author author);

    @Mapping(target = "followerCount", constant = "0L")
    @Mapping(target = "isFollowing", constant = "false")
    @Mapping(target = "books", ignore = true)
    @Mapping(target = "pagination", ignore = true)
    AuthorDetailResponse toAuthorDetailResponse(Author author);

    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "bookAuthors", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Author toAuthorEntity(CreateAuthorRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "bookAuthors", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateAuthorEntityFromDTO(UpdateAuthorRequest request, @MappingTarget Author author);

    @Mapping(target = "authorId", source = "author.authorId")
    @Mapping(target = "name", source = "author.name")
    @Mapping(target = "role", source = "role")
    AuthorRefDTO toAuthorRefDTO(BookAuthor bookAuthor);

    @Mapping(target = "authorId", source = "author.authorId")
    @Mapping(target = "name", source = "author.name")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "photoUrl", source = "author.photoUrl")
    @Mapping(target = "bio", source = "author.bio")
    AuthorDetailRefDTO toAuthorDetailRefDTO(BookAuthor bookAuthor);

    // ── Publisher Mappings ────────────────────────────────────────────────────
    PublisherRefDTO toPublisherRefDTO(Publisher publisher);

    PublisherDTO toPublisherDTO(Publisher publisher);

    @Mapping(target = "books", ignore = true)
    @Mapping(target = "pagination", ignore = true)
    PublisherDetailResponse toPublisherDetailResponse(Publisher publisher);

    @Mapping(target = "publisherId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "books", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Publisher toPublisherEntity(CreatePublisherRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "publisherId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "books", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updatePublisherEntityFromDTO(UpdatePublisherRequest request, @MappingTarget Publisher publisher);

    // ── Category Mappings ─────────────────────────────────────────────────────
    CategoryRefDTO toCategoryRefDTO(Category category);

    @Mapping(target = "bookCount", constant = "0")
    @Mapping(target = "children", source = "children")
    CategoryNodeDTO toCategoryNodeDTO(Category category);

    List<CategoryNodeDTO> toCategoryNodeDTOList(List<Category> categories);

    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "bookCategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Category toCategoryEntity(CreateCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "bookCategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateCategoryEntityFromDTO(UpdateCategoryRequest request, @MappingTarget Category category);

    // ── Format & Price Mappings ───────────────────────────────────────────────
    @Mapping(target = "bookFormatId", source = "format.bookFormatId")
    @Mapping(target = "formatType", source = "format.formatType")
    @Mapping(target = "isbn", source = "format.isbn")
    @Mapping(target = "pageCount", source = "format.pageCount")
    @Mapping(target = "price", source = "price", qualifiedByName = "mapBookPriceToDTO")
    FormatPriceDTO toFormatPriceDTO(BookFormat format, BookPrice price);

    @Named("mapBookPriceToDTO")
    default MoneyDTO mapBookPriceToDTO(BookPrice price) {
        if (price == null || price.getAmount() == null) {
            return null;
        }
        return MoneyDTO.builder()
                .amount(price.getAmount().toPlainString())
                .currency(price.getCurrency() != null ? price.getCurrency() : "INR")
                .build();
    }

    // ── Book Mappings ─────────────────────────────────────────────────────────
    @Mapping(target = "bookId", source = "book.bookId")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "coverImageUrl", source = "book.coverImageUrl")
    @Mapping(target = "averageRating", source = "book.averageRating")
    @Mapping(target = "reviewCount", source = "book.reviewCount")
    @Mapping(target = "isActive", source = "book.active")
    @Mapping(target = "authors", source = "book.bookAuthors")
    @Mapping(target = "formats", source = "formats")
    @Mapping(target = "startingPrice", source = "startingPrice")
    BookSummaryDTO toBookSummaryDTO(Book book, List<FormatPriceDTO> formats, MoneyDTO startingPrice);

    default BookSummaryDTO toSimpleBookSummaryDTO(Book book) {
        if (book == null) {
            return null;
        }
        List<AuthorRefDTO> authorRefs = book.getBookAuthors() != null ?
                book.getBookAuthors().stream()
                        .filter(ba -> ba.getDeletedAt() == null && ba.getAuthor() != null)
                        .map(this::toAuthorRefDTO)
                        .toList() : Collections.emptyList();

        return BookSummaryDTO.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .coverImageUrl(book.getCoverImageUrl())
                .authors(authorRefs)
                .averageRating(book.getAverageRating())
                .reviewCount(book.getReviewCount())
                .isActive(book.isActive())
                .build();
    }

    @Mapping(target = "bookId", source = "book.bookId")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "synopsis", source = "book.synopsis")
    @Mapping(target = "language", source = "book.language")
    @Mapping(target = "coverImageUrl", source = "book.coverImageUrl")
    @Mapping(target = "publishedDate", source = "book.publishedDate")
    @Mapping(target = "salesCount", source = "book.salesCount")
    @Mapping(target = "isActive", source = "book.active")
    @Mapping(target = "publisher", source = "book.publisher")
    @Mapping(target = "authors", source = "authors")
    @Mapping(target = "categories", source = "categories")
    @Mapping(target = "formats", source = "formats")
    @Mapping(target = "startingPrice", source = "startingPrice")
    @Mapping(target = "reviews", source = "reviews")
    @Mapping(target = "deliveryEstimate", source = "deliveryEstimate")
    BookDetailResponse toBookDetailResponse(
            Book book,
            List<AuthorDetailRefDTO> authors,
            List<CategoryRefDTO> categories,
            List<FormatPriceDTO> formats,
            MoneyDTO startingPrice,
            ReviewSummaryDTO reviews,
            DeliveryEstimateDTO deliveryEstimate);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bookId", ignore = true)
    @Mapping(target = "salesCount", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "publisher", ignore = true)
    @Mapping(target = "formats", ignore = true)
    @Mapping(target = "bookAuthors", ignore = true)
    @Mapping(target = "bookCategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateBookEntityFromDTO(UpdateBookRequest request, @MappingTarget Book book);
}
