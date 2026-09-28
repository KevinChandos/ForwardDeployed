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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CouponServiceImpl} using Mockito and AssertJ.
 * <p>
 * Why: Tests discount calculations (FLAT and PERCENT), threshold validation,
 * usage limit checks, and coupon creation/update logic with edge conditions.
 * Side effects: Exercises Coupon business rules without external I/O.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService Unit Tests")
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PromotionsMapper promotionsMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private UUID storeId;
    private Coupon percentCoupon;
    private Coupon flatCoupon;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();

        Store store = new Store();
        store.setStoreId(storeId);
        store.setName("Test Store");

        percentCoupon = new Coupon();
        percentCoupon.setCouponId(UUID.randomUUID());
        percentCoupon.setStore(store);
        percentCoupon.setCode("SAVE10");
        percentCoupon.setDiscountType(Coupon.DiscountType.PERCENT);
        percentCoupon.setDiscountValue(new BigDecimal("10.00"));
        percentCoupon.setMinOrderAmount(new BigDecimal("100.00"));
        percentCoupon.setMaxUses(100);
        percentCoupon.setUsedCount(5);
        percentCoupon.setActive(true);

        flatCoupon = new Coupon();
        flatCoupon.setCouponId(UUID.randomUUID());
        flatCoupon.setStore(store);
        flatCoupon.setCode("FLAT50");
        flatCoupon.setDiscountType(Coupon.DiscountType.FLAT);
        flatCoupon.setDiscountValue(new BigDecimal("50.00"));
        flatCoupon.setMinOrderAmount(BigDecimal.ZERO);
        flatCoupon.setMaxUses(50);
        flatCoupon.setUsedCount(50);
        flatCoupon.setActive(true);
    }

    @Test
    @DisplayName("validateCoupon - Should calculate percentage discount successfully")
    void validateCoupon_shouldCalculatePercentDiscount() {
        // Arrange
        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCode("SAVE10");
        request.setOrderAmount("500.00");

        when(couponRepository.findActiveByStoreAndCode(storeId, "SAVE10")).thenReturn(Optional.of(percentCoupon));

        // Act
        CouponValidationResponse response = couponService.validateCoupon(storeId, request);

        // Assert
        assertThat(response.getIsValid()).isTrue();
        assertThat(response.getCouponCode()).isEqualTo("SAVE10");
        assertThat(response.getDiscountType()).isEqualTo("PERCENT");
        assertThat(response.getDiscountAmount().getAmount()).isEqualTo("50.00");
    }

    @Test
    @DisplayName("validateCoupon - Should return invalid when coupon not found")
    void validateCoupon_shouldReturnInvalid_whenNotFound() {
        // Arrange
        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCode("UNKNOWN");
        request.setOrderAmount("500.00");

        when(couponRepository.findActiveByStoreAndCode(storeId, "UNKNOWN")).thenReturn(Optional.empty());

        // Act
        CouponValidationResponse response = couponService.validateCoupon(storeId, request);

        // Assert
        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getInvalidReason()).contains("does not exist or has expired");
    }

    @Test
    @DisplayName("validateCoupon - Should return invalid when minimum order amount not met")
    void validateCoupon_shouldReturnInvalid_whenMinOrderNotMet() {
        // Arrange
        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCode("SAVE10");
        request.setOrderAmount("50.00");

        when(couponRepository.findActiveByStoreAndCode(storeId, "SAVE10")).thenReturn(Optional.of(percentCoupon));

        // Act
        CouponValidationResponse response = couponService.validateCoupon(storeId, request);

        // Assert
        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getInvalidReason()).contains("Minimum order subtotal");
    }

    @Test
    @DisplayName("validateCoupon - Should return invalid when usage limit reached")
    void validateCoupon_shouldReturnInvalid_whenUsageLimitReached() {
        // Arrange
        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCode("FLAT50");
        request.setOrderAmount("500.00");

        when(couponRepository.findActiveByStoreAndCode(storeId, "FLAT50")).thenReturn(Optional.of(flatCoupon));

        // Act
        CouponValidationResponse response = couponService.validateCoupon(storeId, request);

        // Assert
        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getInvalidReason()).contains("usage limit has been reached");
    }

    @Test
    @DisplayName("createCoupon - Should create coupon successfully")
    void createCoupon_shouldSucceed() {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest();
        request.setCode("NEWYEAR");
        request.setDiscountType(CreateCouponRequest.DiscountTypeEnum.FLAT);
        request.setDiscountValue(100.0);

        Store store = new Store();
        store.setStoreId(storeId);

        Coupon createdEntity = new Coupon();
        createdEntity.setCode("NEWYEAR");

        CouponDTO expectedDto = CouponDTO.builder().code("NEWYEAR").build();

        when(couponRepository.findActiveByStoreAndCode(storeId, "NEWYEAR")).thenReturn(Optional.empty());
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(promotionsMapper.toCouponEntity(request)).thenReturn(createdEntity);
        when(couponRepository.save(any(Coupon.class))).thenReturn(createdEntity);
        when(promotionsMapper.toCouponDTO(createdEntity)).thenReturn(expectedDto);

        // Act
        CouponDTO result = couponService.createCoupon(storeId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("NEWYEAR");
        verify(couponRepository).save(createdEntity);
    }

    @Test
    @DisplayName("createCoupon - Should throw ResourceConflictException when code already exists")
    void createCoupon_shouldThrowConflict_whenDuplicate() {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest();
        request.setCode("SAVE10");

        when(couponRepository.findActiveByStoreAndCode(storeId, "SAVE10")).thenReturn(Optional.of(percentCoupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.createCoupon(storeId, request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getCoupons - Should return paginated coupon response")
    void getCoupons_shouldReturnPagedResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Coupon> couponPage = new PageImpl<>(List.of(percentCoupon), pageable, 1);
        CouponDTO dto = CouponDTO.builder().code("SAVE10").build();

        when(couponRepository.findAllActiveByStoreId(storeId, pageable)).thenReturn(couponPage);
        when(promotionsMapper.toCouponDTOList(List.of(percentCoupon))).thenReturn(List.of(dto));

        // Act
        CouponListResponse response = couponService.getCoupons(storeId, pageable);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(1);
    }
}
