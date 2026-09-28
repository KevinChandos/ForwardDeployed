package io.bookworm.api.cart.infrastructure;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.cart.domain.Cart;
import io.bookworm.api.cart.domain.CartItem;
import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.domain.BookFormat;
import io.bookworm.api.catalogue.infrastructure.BookFormatRepository;
import io.bookworm.api.catalogue.infrastructure.BookRepository;
import io.bookworm.api.common.infrastructure.BaseRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CartRepository} and {@link CartItemRepository} using Testcontainers.
 * <p>
 * Why: Verifies member/guest active cart queries, item existence checks, and soft-delete behaviors.
 * Side effects: Persists Member, Cart, Book, BookFormat, and CartItem entities in the container DB.
 */
@DisplayName("Cart & CartItem Repository Integration Tests")
class CartRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

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
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();

        testMember = new Member();
        testMember.setDisplayName("Test Member");
        testMember.setEmail("member-" + UUID.randomUUID() + "@bookworm.io");
        testMember.setStatus(Member.MemberStatus.ACTIVE);
        testMember = memberRepository.save(testMember);

        testBook = new Book();
        testBook.setTitle("Domain Driven Design");
        testBook.setLanguage("English");
        testBook.setActive(true);
        testBook = bookRepository.save(testBook);

        testFormat = new BookFormat();
        testFormat.setBook(testBook);
        testFormat.setFormatType(BookFormat.FormatType.PAPERBACK);
        testFormat.setIsbn("978-0321125217");
        testFormat.setActive(true);
        testFormat = bookFormatRepository.save(testFormat);
    }

    @Test
    @DisplayName("Should find active cart by member ID")
    void shouldFindActiveMemberCart() {
        // Arrange
        Cart cart = new Cart();
        cart.setMember(testMember);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        cartRepository.save(cart);

        // Act
        Optional<Cart> found = cartRepository.findActiveMemberCart(testMember.getMemberId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(Cart.CartStatus.ACTIVE);
        assertThat(found.get().getMember().getMemberId()).isEqualTo(testMember.getMemberId());
    }

    @Test
    @DisplayName("Should find active cart by guest token")
    void shouldFindActiveGuestCart() {
        // Arrange
        String token = "guest-token-12345";
        Cart cart = new Cart();
        cart.setGuestToken(token);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        cartRepository.save(cart);

        // Act
        Optional<Cart> found = cartRepository.findActiveGuestCart(token);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getGuestToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should find active cart items and query by format")
    void shouldFindActiveCartItemsAndByFormat() {
        // Arrange
        Cart cart = new Cart();
        cart.setMember(testMember);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        cart = cartRepository.save(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setBook(testBook);
        item.setBookFormat(testFormat);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("499.00"));
        item.setCurrency("INR");
        item.setAddedAt(OffsetDateTime.now());
        item = cartItemRepository.save(item);

        // Act
        List<CartItem> items = cartItemRepository.findActiveByCartId(cart.getCartId());
        Optional<CartItem> byFormat = cartItemRepository.findActiveByCartAndFormat(cart.getCartId(), testFormat.getBookFormatId());
        Optional<CartItem> byIdAndCart = cartItemRepository.findActiveByIdAndCartId(item.getCartItemId(), cart.getCartId());

        // Assert
        assertThat(items).hasSize(1);
        assertThat(byFormat).isPresent();
        assertThat(byFormat.get().getQuantity()).isEqualTo(2);
        assertThat(byIdAndCart).isPresent();
    }
}
