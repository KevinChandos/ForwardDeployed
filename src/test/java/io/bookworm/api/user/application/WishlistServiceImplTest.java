package io.bookworm.api.user.application;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.domain.BookFormat;
import io.bookworm.api.catalogue.infrastructure.BookFormatRepository;
import io.bookworm.api.catalogue.infrastructure.BookRepository;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceConflictException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.user.domain.Wishlist;
import io.bookworm.api.user.domain.WishlistItem;
import io.bookworm.api.user.dto.AddWishlistItemRequest;
import io.bookworm.api.user.dto.WishlistItemDTO;
import io.bookworm.api.user.dto.WishlistResponse;
import io.bookworm.api.user.infrastructure.WishlistItemRepository;
import io.bookworm.api.user.infrastructure.WishlistRepository;
import io.bookworm.api.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Unit tests for {@link WishlistServiceImpl} using Mockito and AssertJ.
 * <p>
 * Why: Tests wishlist creation, item additions, duplicate format checks, format availability
 * validations, and soft-delete removal behaviors.
 * Side effects: Validates Wishlist business rules in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistService Unit Tests")
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookFormatRepository bookFormatRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private UUID memberId;
    private UUID bookId;
    private UUID formatId;
    private Member member;
    private Wishlist wishlist;
    private Book book;
    private BookFormat format;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        formatId = UUID.randomUUID();

        member = new Member();
        member.setMemberId(memberId);

        wishlist = new Wishlist();
        wishlist.setWishlistId(UUID.randomUUID());
        wishlist.setMember(member);
        wishlist.setItems(new ArrayList<>());

        book = new Book();
        book.setBookId(bookId);
        book.setTitle("Designing Data-Intensive Applications");

        format = new BookFormat();
        format.setBookFormatId(formatId);
        format.setBook(book);
        format.setFormatType(BookFormat.FormatType.HARDCOVER);
    }

    @Test
    @DisplayName("getWishlist - Should return existing wishlist for member")
    void getWishlist_shouldReturnExistingWishlist() {
        // Arrange
        when(wishlistRepository.findActiveByMemberId(memberId)).thenReturn(Optional.of(wishlist));
        when(userMapper.toWishlistItemDTOList(any())).thenReturn(List.of());

        // Act
        WishlistResponse response = wishlistService.getWishlist(memberId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getWishlistId()).isEqualTo(wishlist.getWishlistId());
        assertThat(response.getItemCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("addWishlistItem - Should add format to wishlist successfully")
    void addWishlistItem_shouldSucceed() {
        // Arrange
        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setBookId(bookId);
        request.setBookFormatId(formatId);

        when(wishlistRepository.findActiveByMemberId(memberId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.existsActiveByWishlistAndFormat(wishlist.getWishlistId(), formatId))
                .thenReturn(false);
        when(bookRepository.findActiveWithDetails(bookId)).thenReturn(Optional.of(book));
        when(bookFormatRepository.findActiveByIdAndBookId(formatId, bookId)).thenReturn(Optional.of(format));
        when(userMapper.toWishlistItemDTOList(any())).thenReturn(List.of(WishlistItemDTO.builder().build()));

        // Act
        WishlistResponse response = wishlistService.addWishlistItem(memberId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItemCount()).isEqualTo(1);
        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("addWishlistItem - Should throw ResourceConflictException if format already in wishlist")
    void addWishlistItem_shouldThrowConflict_whenAlreadyPresent() {
        // Arrange
        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setBookId(bookId);
        request.setBookFormatId(formatId);

        when(wishlistRepository.findActiveByMemberId(memberId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.existsActiveByWishlistAndFormat(wishlist.getWishlistId(), formatId))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> wishlistService.addWishlistItem(memberId, request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already in your wishlist");
    }

    @Test
    @DisplayName("removeWishlistItem - Should soft delete wishlist item")
    void removeWishlistItem_shouldSoftDeleteItem() {
        // Arrange
        UUID itemId = UUID.randomUUID();
        WishlistItem item = new WishlistItem();
        item.setWishlistItemId(itemId);
        item.setWishlist(wishlist);

        when(wishlistRepository.findActiveByMemberId(memberId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findActiveByIdAndWishlistId(itemId, wishlist.getWishlistId()))
                .thenReturn(Optional.of(item));

        // Act
        wishlistService.removeWishlistItem(memberId, itemId);

        // Assert
        assertThat(item.getDeletedAt()).isNotNull();
        verify(wishlistItemRepository).save(item);
    }
}
