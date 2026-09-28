package io.bookworm.api.promotions.application;

import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.dto.PaginationDTO;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceConflictException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.promotions.domain.Coupon;
import io.bookworm.api.promotions.dto.*;
import io.bookworm.api.promotions.infrastructure.CouponRepository;
import io.bookworm.api.promotions.mapper.PromotionsMapper;
import io.bookworm.api.store.domain.Store;
import io.bookworm.api.store.infrastructure.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for Promotions & Coupons bounded context.
 * <p>
 * Why: Orchestrates coupon validation calculations against order totals, expiration enforcement,
 * redemption limit tracking, and administrative coupon CRUD operations.
 * Side effects: Mutates Coupon entity records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final StoreRepository storeRepository;
    private final PromotionsMapper promotionsMapper;

    @Override
    public CouponValidationResponse validateCoupon(UUID storeId, ValidateCouponRequest request) {
        log.info("Validating coupon code: {} for order amount: {}", request.getCode(), request.getOrderAmount());

        UUID resolvedStoreId = resolveStoreId(storeId);
        String code = request.getCode().trim().toUpperCase();
        Optional<Coupon> couponOpt = couponRepository.findActiveByStoreAndCode(resolvedStoreId, code);

        if (couponOpt.isEmpty()) {
            return CouponValidationResponse.builder()
                    .isValid(false)
                    .couponCode(code)
                    .invalidReason("Coupon does not exist or has expired")
                    .build();
        }

        Coupon coupon = couponOpt.get();
        BigDecimal orderAmount = new BigDecimal(request.getOrderAmount());

        if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            return CouponValidationResponse.builder()
                    .isValid(false)
                    .couponCode(code)
                    .invalidReason("Minimum order subtotal of " + coupon.getMinOrderAmount() + " required")
                    .build();
        }

        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            return CouponValidationResponse.builder()
                    .isValid(false)
                    .couponCode(code)
                    .invalidReason("Coupon usage limit has been reached")
                    .build();
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue().min(orderAmount);
        }

        MoneyDTO discountDto = MoneyDTO.builder()
                .amount(discount.toPlainString())
                .currency("INR")
                .build();

        return CouponValidationResponse.builder()
                .isValid(true)
                .couponCode(code)
                .discountType(coupon.getDiscountType().name())
                .discountAmount(discountDto)
                .build();
    }

    @Override
    public CouponListResponse getCoupons(UUID storeId, Pageable pageable) {
        log.info("Fetching coupons for storeId: {}", storeId);
        UUID resolvedStoreId = resolveStoreId(storeId);
        Page<Coupon> page = couponRepository.findAllActiveByStoreId(resolvedStoreId, pageable);

        List<CouponDTO> dtos = promotionsMapper.toCouponDTOList(page.getContent());

        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return CouponListResponse.builder()
                .content(dtos)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public CouponDTO createCoupon(UUID storeId, CreateCouponRequest request) {
        log.info("Creating coupon code: {} for store: {}", request.getCode(), storeId);

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessRuleException("INVALID_COUPON", "Coupon code is required");
        }

        UUID resolvedStoreId = resolveStoreId(storeId);
        String code = request.getCode().trim().toUpperCase();

        if (couponRepository.findActiveByStoreAndCode(resolvedStoreId, code).isPresent()) {
            throw new ResourceConflictException("DUPLICATE_COUPON", "A coupon with this code already exists for this store");
        }

        Store store = storeRepository.findById(resolvedStoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", resolvedStoreId));

        Coupon coupon = promotionsMapper.toCouponEntity(request);
        coupon.setCode(code);
        coupon.setStore(store);
        coupon.setActive(true);

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon {} created successfully", saved.getCouponId());

        return promotionsMapper.toCouponDTO(saved);
    }

    @Override
    @Transactional
    public CouponDTO updateCoupon(UUID couponId, UpdateCouponRequest request) {
        log.info("Updating coupon: {}", couponId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId));

        if (request.getVersion() != null && !request.getVersion().equals(coupon.getVersion())) {
            throw new BusinessRuleException("OPTIMISTIC_LOCK_CONFLICT", "The coupon was modified by another transaction");
        }

        promotionsMapper.updateCouponEntityFromDTO(request, coupon);
        Coupon saved = couponRepository.save(coupon);

        return promotionsMapper.toCouponDTO(saved);
    }

    @Override
    @Transactional
    public void updateCouponStatus(UUID couponId, CouponUpdateStatusRequest request) {
        log.info("Updating status for coupon: {} to active={}", couponId, request.getIsActive());
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId));

        coupon.setActive(Boolean.TRUE.equals(request.getIsActive()));
        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID couponId) {
        log.info("Deleting coupon: {}", couponId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId));

        coupon.setDeletedAt(OffsetDateTime.now());
        couponRepository.save(coupon);
        log.info("Coupon {} soft-deleted", couponId);
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
}
