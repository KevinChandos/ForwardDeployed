package io.bookworm.api.user.infrastructure;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.domain.BookFormat;
import io.bookworm.api.catalogue.infrastructure.BookFormatRepository;
import io.bookworm.api.catalogue.infrastructure.BookRepository;
import io.bookworm.api.common.infrastructure.BaseRepositoryTest;
import io.bookworm.api.user.domain.Wishlist;
import io.bookworm.api.user.domain.WishlistItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WishlistRepository} and {@link WishlistItemRepository} using Testcontainers.
 * <p>
 * Why: Verifies member-owned wishlist lookup, duplicate active format existence checks,
 * and scoped item retrieval in a real PostgreSQL environment.
 * Side effects: Persists Member, Wishlist, Book, BookFormat, and WishlistItem in the test database.
 */
@DisplayName("Wishlist & WishlistItem Repository Integration Tests")
class WishlistRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookFormatRepository bookFormatRepository;

    private Member testMember;
    private Book testBook;
    private BookFormat testFormat;

    @BeforeEach
    void setUp() {
        wishlistItemRepository.deleteAll();
        wishlistRepository.deleteAll();

        testMember = new Member();
        testMember.setDisplayName("Wishlist User");
        testMember.setEmail("wishlist-" + UUID.randomUUID() + "@bookworm.io");
        testMember.setStatus(Member.MemberStatus.ACTIVE);
        testMember = memberRepository.save(testMember);

        testBook = new Book();
        testBook.setTitle("Refactoring");
        testBook.setLanguage("English");
        testBook.setActive(true);
        testBook = bookRepository.save(testBook);

        testFormat = new BookFormat();
        testFormat.setBook(testBook);
        testFormat.setFormatType(BookFormat.FormatType.HARDCOVER);
        testFormat.setIsbn("978-0134757599");
        testFormat.setActive(true);
        testFormat = bookFormatRepository.save(testFormat);
    }

    @Test
    @DisplayName("Should find active wishlist for member")
    void shouldFindActiveByMemberId() {
        // Arrange
        Wishlist wishlist = new Wishlist();
        wishlist.setMember(testMember);
        wishlistRepository.save(wishlist);

        // Act
        Optional<Wishlist> found = wishlistRepository.findActiveByMemberId(testMember.getMemberId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getMember().getMemberId()).isEqualTo(testMember.getMemberId());
    }

    @Test
    @DisplayName("Should check active item existence by wishlist and format")
    void shouldCheckExistsActiveByWishlistAndFormat() {
        // Arrange
        Wishlist wishlist = new Wishlist();
        wishlist.setMember(testMember);
        wishlist = wishlistRepository.save(wishlist);

        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setBook(testBook);
        item.setBookFormat(testFormat);
        item.setAddedAt(OffsetDateTime.now());
        wishlistItemRepository.save(item);

        // Act
        boolean exists = wishlistItemRepository.existsActiveByWishlistAndFormat(wishlist.getWishlistId(), testFormat.getBookFormatId());
        Optional<WishlistItem> byIdAndWishlist = wishlistItemRepository.findActiveByIdAndWishlistId(item.getWishlistItemId(), wishlist.getWishlistId());

        // Assert
        assertThat(exists).isTrue();
        assertThat(byIdAndWishlist).isPresent();
    }
}
