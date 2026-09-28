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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Wishlist bounded context.
 * <p>
 * Why: Orchestrates member wishlist operations, duplicate format checks, format availability
 * validations, and item removals with complete tenant and ownership safety.
 * Side effects: Mutates Wishlist and WishlistItem entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookFormatRepository bookFormatRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public WishlistResponse getWishlist(UUID memberId) {
        log.info("Fetching wishlist for memberId: {}", memberId);
        Wishlist wishlist = getOrCreateWishlist(memberId);
        return buildWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse addWishlistItem(UUID memberId, AddWishlistItemRequest request) {
        log.info("Adding bookId: {}, formatId: {} to member {} wishlist",
                request.getBookId(), request.getBookFormatId(), memberId);

        if (request.getBookId() == null || request.getBookFormatId() == null) {
            throw new BusinessRuleException("INVALID_WISHLIST_ITEM", "bookId and bookFormatId must not be null");
        }

        Wishlist wishlist = getOrCreateWishlist(memberId);

        if (wishlistItemRepository.existsActiveByWishlistAndFormat(wishlist.getWishlistId(), request.getBookFormatId())) {
            log.warn("Item format {} already present in wishlist {}", request.getBookFormatId(), wishlist.getWishlistId());
            throw new ResourceConflictException("ITEM_ALREADY_IN_WISHLIST", "This book edition is already in your wishlist");
        }

        Book book = bookRepository.findActiveWithDetails(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.getBookId()));

        BookFormat format = bookFormatRepository.findActiveByIdAndBookId(request.getBookFormatId(), request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("BookFormat", request.getBookFormatId()));

        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setBook(book);
        item.setBookFormat(format);
        item.setAddedAt(OffsetDateTime.now());

        wishlistItemRepository.save(item);
        wishlist.getItems().add(item);

        log.info("Successfully added item {} to member {} wishlist", item.getWishlistItemId(), memberId);
        return buildWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public void removeWishlistItem(UUID memberId, UUID itemId) {
        log.info("Removing wishlist item: {} for memberId: {}", itemId, memberId);
        Wishlist wishlist = wishlistRepository.findActiveByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist for member", memberId));

        WishlistItem item = wishlistItemRepository.findActiveByIdAndWishlistId(itemId, wishlist.getWishlistId())
                .orElseThrow(() -> new ResourceNotFoundException("WishlistItem", itemId));

        item.setDeletedAt(OffsetDateTime.now());
        wishlistItemRepository.save(item);
        log.info("Successfully soft-deleted wishlist item: {}", itemId);
    }

    private Wishlist getOrCreateWishlist(UUID memberId) {
        return wishlistRepository.findActiveByMemberId(memberId)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setMember(member);
                    return wishlistRepository.save(newWishlist);
                });
    }

    private WishlistResponse buildWishlistResponse(Wishlist wishlist) {
        List<WishlistItem> activeItems = wishlist.getItems() != null ?
                wishlist.getItems().stream()
                        .filter(i -> i.getDeletedAt() == null)
                        .toList() : new ArrayList<>();

        List<WishlistItemDTO> itemDTOs = userMapper.toWishlistItemDTOList(activeItems);

        return WishlistResponse.builder()
                .wishlistId(wishlist.getWishlistId())
                .items(itemDTOs)
                .itemCount(itemDTOs.size())
                .build();
    }
}
