package io.bookworm.api.checkout.application;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.cart.domain.Cart;
import io.bookworm.api.cart.domain.CartItem;
import io.bookworm.api.cart.infrastructure.CartItemRepository;
import io.bookworm.api.cart.infrastructure.CartRepository;
import io.bookworm.api.checkout.domain.*;
import io.bookworm.api.checkout.dto.*;
import io.bookworm.api.checkout.infrastructure.*;
import io.bookworm.api.checkout.mapper.CheckoutMapper;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.dto.PaginationDTO;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.payment.domain.PaymentTransaction;
import io.bookworm.api.payment.domain.WalletAccount;
import io.bookworm.api.payment.domain.WalletTransaction;
import io.bookworm.api.payment.infrastructure.PaymentTransactionRepository;
import io.bookworm.api.payment.infrastructure.WalletAccountRepository;
import io.bookworm.api.payment.infrastructure.WalletTransactionRepository;
import io.bookworm.api.promotions.domain.Coupon;
import io.bookworm.api.promotions.domain.CouponRedemption;
import io.bookworm.api.promotions.infrastructure.CouponRedemptionRepository;
import io.bookworm.api.promotions.infrastructure.CouponRepository;
import io.bookworm.api.shipping.domain.Shipment;
import io.bookworm.api.shipping.infrastructure.ShipmentRepository;
import io.bookworm.api.store.domain.Store;
import io.bookworm.api.store.infrastructure.StoreRepository;
import io.bookworm.api.user.dto.AddressDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for Checkout sessions and Order lifecycle management.
 * <p>
 * Why: Orchestrates multi-step checkout workflows (initiation, address capture, coupon application,
 * wallet redemption, order confirmation) with strict business validations, price calculation,
 * idempotency safety, and order cancellation/returns.
 * Side effects: Mutates CheckoutSession, CheckoutAddress, Order, OrderLine, OrderDeliveryAddress,
 * WalletAccount, WalletTransaction, CouponRedemption, and Cart entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final CheckoutAddressRepository checkoutAddressRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderDeliveryAddressRepository orderDeliveryAddressRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ShipmentRepository shipmentRepository;
    private final StoreRepository storeRepository;
    private final CheckoutMapper checkoutMapper;

    private static final long SESSION_EXPIRY_MINUTES = 30L;
    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("50.00");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% GST

    // ── Checkout Flow ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CheckoutSessionResponse initiateCheckout(UUID memberId, String guestToken, UUID storeId, InitiateCheckoutRequest request) {
        log.info("Initiating checkout session (memberId: {}, guestToken: {})", memberId, guestToken);

        Cart cart;
        Member member = null;
        if (memberId != null) {
            member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
            cart = cartRepository.findActiveMemberCart(memberId)
                    .orElseThrow(() -> new BusinessRuleException("EMPTY_CART", "Cannot initiate checkout with empty cart"));
        } else if (guestToken != null && !guestToken.isBlank()) {
            cart = cartRepository.findActiveGuestCart(guestToken.trim())
                    .orElseThrow(() -> new BusinessRuleException("EMPTY_CART", "Cannot initiate checkout with empty cart"));
        } else {
            throw new BusinessRuleException("INVALID_CHECKOUT", "Either memberId or guestToken must be provided");
        }

        List<CartItem> items = cartItemRepository.findActiveByCartId(cart.getCartId());
        if (items.isEmpty()) {
            throw new BusinessRuleException("EMPTY_CART", "Cart contains no items to checkout");
        }

        // Check if active session already exists for cart
        CheckoutSession session = checkoutSessionRepository.findActiveByCartId(cart.getCartId())
                .orElseGet(() -> {
                    CheckoutSession newSession = new CheckoutSession();
                    newSession.setCart(cart);
                    newSession.setStatus(CheckoutSession.CheckoutStatus.CREATED);
                    newSession.setExpiresAt(OffsetDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));
                    return newSession;
                });

        if (member != null) {
            session.setMember(member);
        } else {
            session.setGuestToken(guestToken.trim());
        }

        cart.setStatus(Cart.CartStatus.CHECKING_OUT);
        cartRepository.save(cart);

        recalculateSessionPricing(session, items);
        CheckoutSession savedSession = checkoutSessionRepository.save(session);
        log.info("Checkout session {} initialized", savedSession.getSessionId());

        return buildSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public CheckoutSessionResponse setDeliveryAddress(UUID sessionId, SetDeliveryAddressRequest request) {
        log.info("Setting delivery address for checkout session: {}", sessionId);
        CheckoutSession session = checkoutSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CheckoutSession", sessionId));

        if (request.getAddress() == null) {
            throw new BusinessRuleException("INVALID_ADDRESS", "Address payload is required");
        }

        // Soft-delete previous checkout address if exists
        checkoutAddressRepository.findActiveBySessionId(sessionId).ifPresent(existing -> {
            existing.setDeletedAt(OffsetDateTime.now());
            checkoutAddressRepository.save(existing);
        });

        AddressDTO dto = request.getAddress();
        CheckoutAddress address = new CheckoutAddress();
        address.setSession(session);
        address.setFirstName(dto.getFirstName());
        address.setLastName(dto.getLastName());
        address.setLine1(dto.getLine1());
        address.setLine2(dto.getLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPinCode(dto.getPinCode());
        address.setCountry(dto.getCountry() != null ? dto.getCountry() : "IN");
        address.setEmail(dto.getEmail());
        address.setPhone(dto.getPhone());
        checkoutAddressRepository.save(address);

        session.setStatus(CheckoutSession.CheckoutStatus.ADDRESS_SET);
        CheckoutSession savedSession = checkoutSessionRepository.save(session);

        return buildSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public CheckoutSessionResponse applyCoupon(UUID sessionId, ApplyCouponRequest request) {
        log.info("Applying coupon '{}' to checkout session: {}", request.getCouponCode(), sessionId);
        CheckoutSession session = checkoutSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CheckoutSession", sessionId));

        if (request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            throw new BusinessRuleException("INVALID_COUPON", "Coupon code must not be blank");
        }

        Store store = getSessionStore(session);
        Coupon coupon = couponRepository.findActiveByStoreAndCode(store.getStoreId(), request.getCouponCode().trim().toUpperCase())
                .orElseThrow(() -> new BusinessRuleException("INVALID_COUPON", "Invalid or expired coupon code"));

        List<CartItem> items = cartItemRepository.findActiveByCartId(session.getCart().getCartId());
        BigDecimal subtotal = calculateItemsSubtotal(items);

        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException("MIN_ORDER_NOT_MET",
                    "Order subtotal does not meet minimum requirement of " + coupon.getMinOrderAmount());
        }

        if (coupon.getMaxRedemptions() != null && coupon.getCurrentRedemptions() >= coupon.getMaxRedemptions()) {
            throw new BusinessRuleException("COUPON_EXHAUSTED", "Coupon usage limit has been reached");
        }

        session.setCoupon(coupon);
        session.setStatus(CheckoutSession.CheckoutStatus.PRICING_APPLIED);
        recalculateSessionPricing(session, items);
        CheckoutSession saved = checkoutSessionRepository.save(session);

        return buildSessionResponse(saved);
    }

    @Override
    @Transactional
    public CheckoutSessionResponse redeemWallet(UUID sessionId, UUID memberId, RedeemWalletRequest request) {
        log.info("Redeeming wallet points for checkout session: {}", sessionId);
        CheckoutSession session = checkoutSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CheckoutSession", sessionId));

        if (memberId == null) {
            throw new BusinessRuleException("WALLET_UNAVAILABLE", "Wallet redemption is only available to registered users");
        }

        WalletAccount wallet = walletAccountRepository.findActiveByMemberId(memberId)
                .orElseThrow(() -> new BusinessRuleException("WALLET_NOT_FOUND", "Member wallet account not found"));

        BigDecimal requestedPoints = request.getPointsToRedeem() != null ?
                new BigDecimal(request.getPointsToRedeem().getAmount()) : BigDecimal.ZERO;

        if (requestedPoints.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "Points to redeem cannot be negative");
        }
        if (requestedPoints.compareTo(wallet.getBalance()) > 0) {
            throw new BusinessRuleException("INSUFFICIENT_WALLET_BALANCE", "Insufficient wallet balance");
        }

        List<CartItem> items = cartItemRepository.findActiveByCartId(session.getCart().getCartId());
        BigDecimal subtotal = calculateItemsSubtotal(items);
        BigDecimal discountFromCoupon = session.getDiscountAmount() != null ? session.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal maxApplicable = subtotal.subtract(discountFromCoupon).max(BigDecimal.ZERO);

        BigDecimal applicablePoints = requestedPoints.min(maxApplicable);
        session.setWalletDebitAmount(applicablePoints);
        recalculateSessionPricing(session, items);
        CheckoutSession saved = checkoutSessionRepository.save(session);

        return buildSessionResponse(saved);
    }

    @Override
    @Transactional
    public OrderConfirmationResponse confirmOrder(UUID sessionId, UUID memberId, String guestToken, String idempotencyKey) {
        log.info("Confirming order for session: {}, idempotencyKey: {}", sessionId, idempotencyKey);
        CheckoutSession session = checkoutSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CheckoutSession", sessionId));

        CheckoutAddress address = checkoutAddressRepository.findActiveBySessionId(sessionId)
                .orElseThrow(() -> new BusinessRuleException("ADDRESS_REQUIRED", "Delivery address must be set prior to confirmation"));

        List<CartItem> items = cartItemRepository.findActiveByCartId(session.getCart().getCartId());
        if (items.isEmpty()) {
            throw new BusinessRuleException("EMPTY_CART", "Cart contains no items to place order");
        }

        Store store = getSessionStore(session);
        recalculateSessionPricing(session, items);

        Order order = new Order();
        if (session.getMember() != null) {
            order.setMember(session.getMember());
        } else {
            order.setGuestEmail(address.getEmail());
        }
        order.setStore(store);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setSubtotal(session.getSubtotal());
        order.setTaxAmount(session.getTaxAmount());
        order.setShippingAmount(session.getShippingAmount());
        order.setDiscountAmount(session.getDiscountAmount());
        order.setGrandTotal(session.getGrandTotal());
        order.setCurrency(session.getCurrency());
        order.setCoupon(session.getCoupon());
        order.setCouponCodeSnapshot(session.getCoupon() != null ? session.getCoupon().getCode() : null);
        order.setWalletDebitAmount(session.getWalletDebitAmount());
        order.setPlacedAt(OffsetDateTime.now());
        order.setConfirmedAt(OffsetDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Snapshot delivery address
        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress();
        deliveryAddress.setOrder(savedOrder);
        deliveryAddress.setFirstName(address.getFirstName());
        deliveryAddress.setLastName(address.getLastName());
        deliveryAddress.setLine1(address.getLine1());
        deliveryAddress.setLine2(address.getLine2());
        deliveryAddress.setCity(address.getCity());
        deliveryAddress.setState(address.getState());
        deliveryAddress.setPinCode(address.getPinCode());
        deliveryAddress.setCountry(address.getCountry());
        deliveryAddress.setEmail(address.getEmail());
        deliveryAddress.setPhone(address.getPhone());
        orderDeliveryAddressRepository.save(deliveryAddress);
        savedOrder.setDeliveryAddress(deliveryAddress);

        // Snapshot order lines
        List<OrderLine> orderLines = new ArrayList<>();
        for (CartItem item : items) {
            OrderLine line = new OrderLine();
            line.setOrder(savedOrder);
            line.setBook(item.getBook());
            line.setBookFormat(item.getBookFormat());
            line.setTitleSnapshot(item.getBook().getTitle());
            line.setAuthorSnapshot(item.getBook().getBookAuthors() != null && !item.getBook().getBookAuthors().isEmpty() ?
                    item.getBook().getBookAuthors().get(0).getAuthor().getName() : "Unknown");
            line.setFormatSnapshot(item.getBookFormat().getFormatType().name());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getUnitPrice());
            line.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            line.setCurrency(item.getCurrency());
            orderLineRepository.save(line);
            orderLines.add(line);

            // Soft-delete cart item
            item.setDeletedAt(OffsetDateTime.now());
            cartItemRepository.save(item);
        }
        savedOrder.setLines(orderLines);

        // Handle coupon redemption record
        if (session.getCoupon() != null) {
            Coupon coupon = session.getCoupon();
            coupon.setCurrentRedemptions(coupon.getCurrentRedemptions() + 1);
            couponRepository.save(coupon);

            CouponRedemption redemption = new CouponRedemption();
            redemption.setCoupon(coupon);
            redemption.setMember(session.getMember());
            redemption.setOrder(savedOrder);
            redemption.setDiscountApplied(session.getDiscountAmount());
            couponRedemptionRepository.save(redemption);
        }

        // Handle wallet debit transaction
        if (session.getWalletDebitAmount().compareTo(BigDecimal.ZERO) > 0 && session.getMember() != null) {
            WalletAccount wallet = walletAccountRepository.findActiveByMemberId(session.getMember().getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", session.getMember().getMemberId()));
            wallet.setBalance(wallet.getBalance().subtract(session.getWalletDebitAmount()));
            walletAccountRepository.save(wallet);

            WalletTransaction txn = new WalletTransaction();
            txn.setWalletAccount(wallet);
            txn.setType(WalletTransaction.TransactionType.DEBIT);
            txn.setAmount(session.getWalletDebitAmount());
            txn.setRunningBalance(wallet.getBalance());
            txn.setReferenceType(WalletTransaction.ReferenceType.ORDER);
            txn.setReferenceId(savedOrder.getOrderId());
            txn.setDescription("Order redemption #" + savedOrder.getOrderId());
            walletTransactionRepository.save(txn);
        }

        // Update session & cart status
        session.setStatus(CheckoutSession.CheckoutStatus.COMPLETED);
        checkoutSessionRepository.save(session);

        Cart cart = session.getCart();
        cart.setStatus(Cart.CartStatus.CONVERTED);
        cartRepository.save(cart);

        log.info("Order {} confirmed successfully", savedOrder.getOrderId());
        return buildOrderConfirmationResponse(savedOrder, orderLines, deliveryAddress);
    }

    // ── Order Management ───────────────────────────────────────────────────────

    @Override
    public OrderDetailResponse getOrderById(UUID orderId, UUID memberId) {
        log.info("Fetching order details for orderId: {}, memberId: {}", orderId, memberId);
        Order order;
        if (memberId != null) {
            order = orderRepository.findActiveByIdAndMemberId(orderId, memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        } else {
            order = orderRepository.findActive(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        }

        List<OrderLine> lines = orderLineRepository.findActiveByOrderId(orderId);
        List<OrderLineDTO> lineDTOs = checkoutMapper.toOrderLineDTOList(lines);
        AddressDTO addressDTO = order.getDeliveryAddress() != null ?
                checkoutMapper.toAddressDTO(order.getDeliveryAddress()) : null;

        PriceSummaryDTO priceSummary = PriceSummaryDTO.builder()
                .itemsSubtotal(checkoutMapper.amountToMoneyDTO(order.getSubtotal()))
                .shippingCost(checkoutMapper.amountToMoneyDTO(order.getShippingAmount()))
                .couponDiscount(checkoutMapper.amountToMoneyDTO(order.getDiscountAmount()))
                .walletDiscount(checkoutMapper.amountToMoneyDTO(order.getWalletDebitAmount()))
                .taxAmount(checkoutMapper.amountToMoneyDTO(order.getTaxAmount()))
                .totalPayable(checkoutMapper.amountToMoneyDTO(order.getGrandTotal()))
                .build();

        PaymentSummaryDTO payment = paymentTransactionRepository.findLatestByOrderId(orderId)
                .map(t -> PaymentSummaryDTO.builder()
                        .transactionId(t.getTransactionId())
                        .status(t.getStatus().name())
                        .paymentMethod(t.getPaymentMethod().name())
                        .amount(checkoutMapper.amountToMoneyDTO(t.getAmount()))
                        .build())
                .orElse(null);

        ShipmentSummaryDTO shipment = shipmentRepository.findActiveByOrderId(orderId)
                .map(s -> ShipmentSummaryDTO.builder()
                        .shipmentId(s.getShipmentId())
                        .status(s.getStatus().name())
                        .trackingNumber(s.getTrackingNumber())
                        .carrier(s.getCarrier())
                        .estimatedDelivery(s.getEstimatedDelivery() != null ? s.getEstimatedDelivery().toLocalDate() : null)
                        .build())
                .orElse(null);

        List<ReturnRequest> returns = returnRequestRepository.findActiveByOrderId(orderId);
        ReturnRequestDTO returnDTO = returns.isEmpty() ? null : checkoutMapper.toReturnRequestDTO(returns.get(0));

        return checkoutMapper.toOrderDetailResponse(order, lineDTOs, addressDTO, priceSummary, payment, shipment, returnDTO);
    }

    @Override
    public OrderListResponse getMemberOrders(UUID memberId, Pageable pageable) {
        log.info("Paging member orders for memberId: {}", memberId);
        Page<Order> page = orderRepository.findActiveByMemberId(memberId, pageable);

        List<OrderSummaryDTO> summaries = page.getContent().stream().map(o -> {
            List<OrderLine> lines = orderLineRepository.findActiveByOrderId(o.getOrderId());
            int itemCount = lines.stream().mapToInt(OrderLine::getQuantity).sum();
            String pStatus = paymentTransactionRepository.findLatestByOrderId(o.getOrderId())
                    .map(t -> t.getStatus().name()).orElse("PENDING");
            String sStatus = shipmentRepository.findActiveByOrderId(o.getOrderId())
                    .map(s -> s.getStatus().name()).orElse("NOT_SHIPPED");
            return checkoutMapper.toOrderSummaryDTO(o, itemCount, pStatus, sStatus);
        }).toList();

        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return OrderListResponse.builder()
                .content(summaries)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId, UUID memberId, CancelOrderRequest request) {
        log.info("Processing order cancellation for orderId: {}", orderId);
        Order order = orderRepository.findActiveByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT &&
            order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT &&
            order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new BusinessRuleException("CANNOT_CANCEL_ORDER",
                    "Order cannot be cancelled in state: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(OffsetDateTime.now());
        order.setCancellationReason(request.getReason());
        orderRepository.save(order);
        log.info("Order {} marked CANCELLED", orderId);
    }

    @Override
    @Transactional
    public ReturnRequestDTO requestReturn(UUID orderId, UUID memberId, ReturnRequestBody request) {
        log.info("Processing return request for orderId: {}", orderId);
        Order order = orderRepository.findActiveByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new BusinessRuleException("CANNOT_RETURN_ORDER",
                    "Returns can only be requested for DELIVERED orders");
        }

        ReturnRequest returnReq = new ReturnRequest();
        returnReq.setOrder(order);
        returnReq.setStatus(ReturnRequest.ReturnStatus.PENDING);
        returnReq.setReason(request.getReason());
        returnReq.setRefundAmount(order.getGrandTotal());
        returnReq.setRequestedAt(OffsetDateTime.now());

        ReturnRequest saved = returnRequestRepository.save(returnReq);
        order.setStatus(Order.OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        return checkoutMapper.toReturnRequestDTO(saved);
    }

    // ── Pricing Computation Helpers ────────────────────────────────────────────

    private void recalculateSessionPricing(CheckoutSession session, List<CartItem> items) {
        BigDecimal subtotal = calculateItemsSubtotal(items);
        BigDecimal discount = BigDecimal.ZERO;

        if (session.getCoupon() != null) {
            Coupon c = session.getCoupon();
            if (c.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
                discount = subtotal.multiply(c.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (c.getMaxDiscountAmount() != null) {
                    discount = discount.min(c.getMaxDiscountAmount());
                }
            } else {
                discount = c.getDiscountValue().min(subtotal);
            }
        }

        BigDecimal walletDebit = session.getWalletDebitAmount() != null ? session.getWalletDebitAmount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount).subtract(walletDebit).max(BigDecimal.ZERO);
        BigDecimal taxAmount = taxableAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingCost = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : DEFAULT_SHIPPING_FEE;
        BigDecimal grandTotal = taxableAmount.add(taxAmount).add(shippingCost);

        session.setSubtotal(subtotal);
        session.setDiscountAmount(discount);
        session.setTaxAmount(taxAmount);
        session.setShippingAmount(shippingCost);
        session.setGrandTotal(grandTotal);
        session.setCurrency("INR");
    }

    private BigDecimal calculateItemsSubtotal(List<CartItem> items) {
        return items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Store getSessionStore(CheckoutSession session) {
        return storeRepository.findActiveDefaultStore()
                .orElseGet(() -> {
                    List<Store> all = storeRepository.findAll();
                    return all.isEmpty() ? null : all.get(0);
                });
    }

    private CheckoutSessionResponse buildSessionResponse(CheckoutSession session) {
        CheckoutAddress address = checkoutAddressRepository.findActiveBySessionId(session.getSessionId()).orElse(null);
        AddressDTO addressDTO = address != null ? checkoutMapper.toAddressDTO(address) : null;

        CouponSummaryDTO couponDTO = session.getCoupon() != null ?
                CouponSummaryDTO.builder()
                        .code(session.getCoupon().getCode())
                        .discountType(session.getCoupon().getDiscountType().name())
                        .discountValue(session.getCoupon().getDiscountValue().toPlainString())
                        .build() : null;

        PriceSummaryDTO priceSummary = PriceSummaryDTO.builder()
                .itemsSubtotal(checkoutMapper.amountToMoneyDTO(session.getSubtotal()))
                .shippingCost(checkoutMapper.amountToMoneyDTO(session.getShippingAmount()))
                .couponDiscount(checkoutMapper.amountToMoneyDTO(session.getDiscountAmount()))
                .walletDiscount(checkoutMapper.amountToMoneyDTO(session.getWalletDebitAmount()))
                .taxAmount(checkoutMapper.amountToMoneyDTO(session.getTaxAmount()))
                .totalPayable(checkoutMapper.amountToMoneyDTO(session.getGrandTotal()))
                .build();

        return CheckoutSessionResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus().name())
                .deliveryAddress(addressDTO)
                .appliedCoupon(couponDTO)
                .walletPointsRedeemed(checkoutMapper.amountToMoneyDTO(session.getWalletDebitAmount()))
                .priceSummary(priceSummary)
                .expiresAt(session.getExpiresAt())
                .build();
    }

    private OrderConfirmationResponse buildOrderConfirmationResponse(Order order, List<OrderLine> lines, OrderDeliveryAddress address) {
        List<OrderLineDTO> lineDTOs = checkoutMapper.toOrderLineDTOList(lines);
        AddressDTO addressDTO = checkoutMapper.toAddressDTO(address);

        PriceSummaryDTO priceSummary = PriceSummaryDTO.builder()
                .itemsSubtotal(checkoutMapper.amountToMoneyDTO(order.getSubtotal()))
                .shippingCost(checkoutMapper.amountToMoneyDTO(order.getShippingAmount()))
                .couponDiscount(checkoutMapper.amountToMoneyDTO(order.getDiscountAmount()))
                .walletDiscount(checkoutMapper.amountToMoneyDTO(order.getWalletDebitAmount()))
                .taxAmount(checkoutMapper.amountToMoneyDTO(order.getTaxAmount()))
                .totalPayable(checkoutMapper.amountToMoneyDTO(order.getGrandTotal()))
                .build();

        return checkoutMapper.toOrderConfirmationResponse(order, lineDTOs, addressDTO, priceSummary, null, null);
    }
}
