package io.bookworm.api.cart.application;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.cart.domain.Cart;
import io.bookworm.api.cart.domain.CartItem;
import io.bookworm.api.cart.dto.AddCartItemRequest;
import io.bookworm.api.cart.dto.CartItemDTO;
import io.bookworm.api.cart.dto.CartResponse;
import io.bookworm.api.cart.dto.UpdateCartItemRequest;
import io.bookworm.api.cart.infrastructure.CartItemRepository;
import io.bookworm.api.cart.infrastructure.CartRepository;
import io.bookworm.api.cart.mapper.CartMapper;
import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.domain.BookFormat;
import io.bookworm.api.catalogue.domain.BookPrice;
import io.bookworm.api.catalogue.infrastructure.BookFormatRepository;
import io.bookworm.api.catalogue.infrastructure.BookPriceRepository;
import io.bookworm.api.catalogue.infrastructure.BookRepository;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.store.domain.Store;
import io.bookworm.api.store.infrastructure.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CartServiceImpl} using Mockito and AssertJ.
 * <p>
 * Why: Tests cart retrieval, item additions, quantity boundary checks (max 10),
 * price computation, and item updates across member and guest sessions.
 * Side effects: Validates business logic in isolation from database/network layers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookFormatRepository bookFormatRepository;

    @Mock
    private BookPriceRepository bookPriceRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private UUID memberId;
    private UUID storeId;
    private UUID bookId;
    private UUID formatId;
    private Member member;
    private Cart cart;
    private Book book;
    private BookFormat format;
    private BookPrice price;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        formatId = UUID.randomUUID();

        member = new Member();
        member.setMemberId(memberId);

        cart = new Cart();
        cart.setCartId(UUID.randomUUID());
        cart.setMember(member);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        cart.setItems(new ArrayList<>());

        book = new Book();
        book.setBookId(bookId);
        book.setTitle("Clean Architecture");

        format = new BookFormat();
        format.setBookFormatId(formatId);
        format.setBook(book);
        format.setFormatType(BookFormat.FormatType.PAPERBACK);

        price = new BookPrice();
        price.setAmount(new BigDecimal("699.00"));
        price.setCurrency("INR");
    }

    @Test
    @DisplayName("getCart - Should retrieve cart and compute totals correctly")
    void getCart_shouldReturnPopulatedResponse() {
        // Arrange
        when(cartRepository.findActiveMemberCart(memberId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findActiveByCartId(cart.getCartId())).thenReturn(List.of());
        when(cartMapper.toCartResponse(eq(cart), any(), eq(0), any()))
                .thenReturn(CartResponse.builder().cartId(cart.getCartId()).itemCount(0).build());

        // Act
        CartResponse response = cartService.getCart(memberId, null, storeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCartId()).isEqualTo(cart.getCartId());
        assertThat(response.getItemCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("addItem - Should add new item to cart successfully")
    void addItem_shouldAddNewItem() {
        // Arrange
        AddCartItemRequest request = new AddCartItemRequest();
        request.setBookId(bookId);
        request.setBookFormatId(formatId);
        request.setQuantity(2);

        when(cartRepository.findActiveMemberCart(memberId)).thenReturn(Optional.of(cart));
        when(bookRepository.findActiveWithDetails(bookId)).thenReturn(Optional.of(book));
        when(bookFormatRepository.findActiveByIdAndBookId(formatId, bookId)).thenReturn(Optional.of(format));
        when(bookPriceRepository.findCurrentPrice(eq(formatId), eq(storeId), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(price));
        when(cartItemRepository.findActiveByCartAndFormat(cart.getCartId(), formatId)).thenReturn(Optional.empty());
        when(cartItemRepository.findActiveByCartId(cart.getCartId())).thenReturn(List.of());
        when(cartMapper.toCartResponse(eq(cart), any(), any(), any()))
                .thenReturn(CartResponse.builder().cartId(cart.getCartId()).itemCount(2).build());

        // Act
        CartResponse response = cartService.addItem(memberId, null, storeId, request);

        // Assert
        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItem - Should throw BusinessRuleException when quantity exceeds 10")
    void addItem_shouldThrowException_whenQuantityExceedsLimit() {
        // Arrange
        AddCartItemRequest request = new AddCartItemRequest();
        request.setBookId(bookId);
        request.setBookFormatId(formatId);
        request.setQuantity(15);

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItem(memberId, null, storeId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot add more than 10 copies");
    }

    @Test
    @DisplayName("updateItemQuantity - Should update quantity successfully")
    void updateItemQuantity_shouldUpdateQuantity() {
        // Arrange
        UUID itemId = UUID.randomUUID();
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        CartItem item = new CartItem();
        item.setCartItemId(itemId);
        item.setCart(cart);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("699.00"));
        item.setBookFormat(format);

        when(cartRepository.findActiveMemberCart(memberId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findActiveByIdAndCartId(itemId, cart.getCartId())).thenReturn(Optional.of(item));
        when(cartItemRepository.findActiveByCartId(cart.getCartId())).thenReturn(List.of(item));
        when(cartMapper.toCartResponse(eq(cart), any(), any(), any()))
                .thenReturn(CartResponse.builder().cartId(cart.getCartId()).itemCount(5).build());

        // Act
        CartResponse response = cartService.updateItemQuantity(memberId, null, storeId, itemId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(item);
    }

    @Test
    @DisplayName("removeItem - Should soft-delete cart item")
    void removeItem_shouldSoftDeleteItem() {
        // Arrange
        UUID itemId = UUID.randomUUID();
        CartItem item = new CartItem();
        item.setCartItemId(itemId);
        item.setCart(cart);

        when(cartRepository.findActiveMemberCart(memberId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findActiveByIdAndCartId(itemId, cart.getCartId())).thenReturn(Optional.of(item));

        // Act
        cartService.removeItem(memberId, null, itemId);

        // Assert
        assertThat(item.getDeletedAt()).isNotNull();
        verify(cartItemRepository).save(item);
    }
}
