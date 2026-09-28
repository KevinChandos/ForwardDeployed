package io.bookworm.api.cart.application;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.cart.domain.Cart;
import io.bookworm.api.cart.domain.CartItem;
import io.bookworm.api.cart.dto.*;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service implementation for Shopping Cart operations.
 * <p>
 * Why: Handles cart creation and lookup for both guests (via guest token) and authenticated members,
 * item addition/quantity bounds checking (max 10), price recalculation based on current store prices,
 * and cart merging upon member login.
 * Side effects: Mutates Cart and CartItem entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookFormatRepository bookFormatRepository;
    private final BookPriceRepository bookPriceRepository;
    private final StoreRepository storeRepository;
    private final CartMapper cartMapper;

    private static final int MAX_ITEM_QUANTITY = 10;

    @Override
    @Transactional
    public CartResponse getCart(UUID memberId, String guestToken, UUID storeId) {
        log.info("Fetching cart for memberId: {}, guestToken: {}, storeId: {}", memberId, guestToken, storeId);
        Cart cart = getOrCreateCart(memberId, guestToken);
        return buildCartResponse(cart, storeId);
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID memberId, String guestToken, UUID storeId, AddCartItemRequest request) {
        log.info("Adding item to cart (bookId: {}, formatId: {}, quantity: {})",
                request.getBookId(), request.getBookFormatId(), request.getQuantity());

        if (request.getBookId() == null || request.getBookFormatId() == null) {
            throw new BusinessRuleException("INVALID_CART_ITEM", "bookId and bookFormatId must not be null");
        }
        int quantityToAdd = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        if (quantityToAdd > MAX_ITEM_QUANTITY) {
            throw new BusinessRuleException("QUANTITY_LIMIT_EXCEEDED", "Cannot add more than " + MAX_ITEM_QUANTITY + " copies per item");
        }

        Cart cart = getOrCreateCart(memberId, guestToken);
        Book book = bookRepository.findActiveWithDetails(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.getBookId()));

        BookFormat format = bookFormatRepository.findActiveByIdAndBookId(request.getBookFormatId(), request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("BookFormat", request.getBookFormatId()));

        UUID resolvedStoreId = resolveStoreId(storeId);
        BookPrice price = bookPriceRepository.findCurrentPrice(request.getBookFormatId(), resolvedStoreId, OffsetDateTime.now())
                .orElseThrow(() -> new BusinessRuleException("PRICE_NOT_FOUND", "No current price found for this format"));

        Optional<CartItem> existingItemOpt = cartItemRepository.findActiveByCartAndFormat(cart.getCartId(), request.getBookFormatId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantityToAdd;
            if (newQuantity > MAX_ITEM_QUANTITY) {
                throw new BusinessRuleException("QUANTITY_LIMIT_EXCEEDED", "Total quantity cannot exceed " + MAX_ITEM_QUANTITY);
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPrice(price.getAmount());
            cartItemRepository.save(existingItem);
            log.info("Incremented quantity of item {} to {}", existingItem.getCartItemId(), newQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setBook(book);
            newItem.setBookFormat(format);
            newItem.setQuantity(quantityToAdd);
            newItem.setUnitPrice(price.getAmount());
            newItem.setCurrency(price.getCurrency() != null ? price.getCurrency() : "INR");
            newItem.setAddedAt(OffsetDateTime.now());
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
            log.info("Created new cart item {} with quantity {}", newItem.getCartItemId(), quantityToAdd);
        }

        return buildCartResponse(cart, resolvedStoreId);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(UUID memberId, String guestToken, UUID storeId, UUID itemId, UpdateCartItemRequest request) {
        log.info("Updating quantity for cart item: {} to {}", itemId, request.getQuantity());

        if (request.getQuantity() == null || request.getQuantity() < 1 || request.getQuantity() > MAX_ITEM_QUANTITY) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity must be between 1 and " + MAX_ITEM_QUANTITY);
        }

        Cart cart = getOrCreateCart(memberId, guestToken);
        CartItem item = cartItemRepository.findActiveByIdAndCartId(itemId, cart.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart, resolveStoreId(storeId));
    }

    @Override
    @Transactional
    public void removeItem(UUID memberId, String guestToken, UUID itemId) {
        log.info("Removing cart item: {}", itemId);
        Cart cart = getOrCreateCart(memberId, guestToken);
        CartItem item = cartItemRepository.findActiveByIdAndCartId(itemId, cart.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        item.setDeletedAt(OffsetDateTime.now());
        cartItemRepository.save(item);
        log.info("Soft-deleted cart item: {}", itemId);
    }

    @Override
    @Transactional
    public CartResponse mergeCart(UUID memberId, UUID storeId, MergeCartRequest request) {
        log.info("Merging guest cart token {} into member {} cart", request.getGuestToken(), memberId);

        if (request.getGuestToken() == null || request.getGuestToken().isBlank()) {
            throw new BusinessRuleException("INVALID_GUEST_TOKEN", "Guest token is required for cart merge");
        }

        Cart memberCart = getOrCreateCart(memberId, null);
        Optional<Cart> guestCartOpt = cartRepository.findActiveGuestCart(request.getGuestToken().trim());

        if (guestCartOpt.isPresent()) {
            Cart guestCart = guestCartOpt.get();
            List<CartItem> guestItems = cartItemRepository.findActiveByCartId(guestCart.getCartId());

            for (CartItem gItem : guestItems) {
                Optional<CartItem> mItemOpt = cartItemRepository.findActiveByCartAndFormat(
                        memberCart.getCartId(), gItem.getBookFormat().getBookFormatId());

                if (mItemOpt.isPresent()) {
                    CartItem mItem = mItemOpt.get();
                    int combined = Math.min(MAX_ITEM_QUANTITY, mItem.getQuantity() + gItem.getQuantity());
                    mItem.setQuantity(combined);
                    cartItemRepository.save(mItem);
                } else {
                    CartItem newItem = new CartItem();
                    newItem.setCart(memberCart);
                    newItem.setBook(gItem.getBook());
                    newItem.setBookFormat(gItem.getBookFormat());
                    newItem.setQuantity(Math.min(MAX_ITEM_QUANTITY, gItem.getQuantity()));
                    newItem.setUnitPrice(gItem.getUnitPrice());
                    newItem.setCurrency(gItem.getCurrency());
                    newItem.setAddedAt(OffsetDateTime.now());
                    cartItemRepository.save(newItem);
                    memberCart.getItems().add(newItem);
                }
                gItem.setDeletedAt(OffsetDateTime.now());
                cartItemRepository.save(gItem);
            }

            guestCart.setStatus(Cart.CartStatus.MERGED);
            cartRepository.save(guestCart);
            log.info("Guest cart {} merged and marked MERGED", guestCart.getCartId());
        }

        return buildCartResponse(memberCart, resolveStoreId(storeId));
    }

    private Cart getOrCreateCart(UUID memberId, String guestToken) {
        if (memberId != null) {
            return cartRepository.findActiveMemberCart(memberId)
                    .orElseGet(() -> {
                        Member member = memberRepository.findById(memberId)
                                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
                        Cart newCart = new Cart();
                        newCart.setMember(member);
                        newCart.setStatus(Cart.CartStatus.ACTIVE);
                        return cartRepository.save(newCart);
                    });
        } else if (guestToken != null && !guestToken.isBlank()) {
            return cartRepository.findActiveGuestCart(guestToken.trim())
                    .orElseGet(() -> {
                        Cart newCart = new Cart();
                        newCart.setGuestToken(guestToken.trim());
                        newCart.setStatus(Cart.CartStatus.ACTIVE);
                        return cartRepository.save(newCart);
                    });
        }
        throw new BusinessRuleException("INVALID_IDENTIFIER", "Either memberId or guestToken must be provided");
    }

    private UUID resolveStoreId(UUID storeId) {
        if (storeId != null) {
            return storeId;
        }
        return storeRepository.findActiveDefaultStore()
                .map(Store::getStoreId)
                .orElseGet(() -> {
                    List<Store> all = storeRepository.findAll();
                    return all.isEmpty() ? null : all.get(0).getStoreId();
                });
    }

    private CartResponse buildCartResponse(Cart cart, UUID storeId) {
        List<CartItem> items = cartItemRepository.findActiveByCartId(cart.getCartId());
        List<CartItemDTO> itemDTOs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItemCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (CartItem item : items) {
            BigDecimal effectivePrice = item.getUnitPrice();
            if (storeId != null) {
                Optional<BookPrice> currentPrice = bookPriceRepository.findCurrentPrice(
                        item.getBookFormat().getBookFormatId(), storeId, now);
                if (currentPrice.isPresent()) {
                    effectivePrice = currentPrice.get().getAmount();
                }
            }

            BigDecimal itemSubtotal = effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemSubtotal);
            totalItemCount += item.getQuantity();

            MoneyDTO unitPriceDto = MoneyDTO.builder().amount(effectivePrice.toPlainString()).currency(item.getCurrency()).build();
            MoneyDTO subtotalDto = MoneyDTO.builder().amount(itemSubtotal.toPlainString()).currency(item.getCurrency()).build();

            itemDTOs.add(cartMapper.toCartItemDTO(item, unitPriceDto, subtotalDto));
        }

        MoneyDTO cartSubtotal = MoneyDTO.builder()
                .amount(totalAmount.toPlainString())
                .currency("INR")
                .build();

        return cartMapper.toCartResponse(cart, itemDTOs, totalItemCount, cartSubtotal);
    }
}
