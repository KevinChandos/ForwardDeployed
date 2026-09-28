package io.bookworm.api.promotions.infrastructure;

import io.bookworm.api.common.infrastructure.BaseRepositoryTest;
import io.bookworm.api.promotions.domain.Coupon;
import io.bookworm.api.store.domain.Store;
import io.bookworm.api.store.infrastructure.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CouponRepository} using PostgreSQL Testcontainers.
 * <p>
 * Why: Verifies complex custom JPQL queries such as active lookup with expiration checks
 * and store-scoped pagination against a real database instance.
 * Side effects: Persists and queries Coupon and Store entities in the Testcontainer DB.
 */
@DisplayName("CouponRepository Integration Tests")
class CouponRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private StoreRepository storeRepository;

    private Store testStore;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();

        testStore = new Store();
        testStore.setName("Main Test Store");
        testStore.setSlug("test-store-" + UUID.randomUUID());
        testStore.setStatus(Store.StoreStatus.ACTIVE);
        testStore = storeRepository.save(testStore);
    }

    @Test
    @DisplayName("Should find active coupon by store ID and uppercase code when not expired")
    void shouldFindActiveByStoreAndCode_whenValid() {
        // Arrange
        Coupon coupon = new Coupon();
        coupon.setStore(testStore);
        coupon.setCode("SAVE20");
        coupon.setDescription("20% off");
        coupon.setDiscountType(Coupon.DiscountType.PERCENT);
        coupon.setDiscountValue(new BigDecimal("20.00"));
        coupon.setMinOrderAmount(new BigDecimal("100.00"));
        coupon.setActive(true);
        coupon.setExpiresAt(OffsetDateTime.now().plusDays(5));
        couponRepository.save(coupon);

        // Act
        Optional<Coupon> found = couponRepository.findActiveByStoreAndCode(testStore.getStoreId(), "SAVE20");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("SAVE20");
        assertThat(found.get().getDiscountValue()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("Should not return coupon if it is expired")
    void shouldNotFindActiveByStoreAndCode_whenExpired() {
        // Arrange
        Coupon coupon = new Coupon();
        coupon.setStore(testStore);
        coupon.setCode("EXPIRED50");
        coupon.setDiscountType(Coupon.DiscountType.FLAT);
        coupon.setDiscountValue(new BigDecimal("50.00"));
        coupon.setActive(true);
        coupon.setExpiresAt(OffsetDateTime.now().minusDays(1));
        couponRepository.save(coupon);

        // Act
        Optional<Coupon> found = couponRepository.findActiveByStoreAndCode(testStore.getStoreId(), "EXPIRED50");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should not return coupon if isActive is false")
    void shouldNotFindActiveByStoreAndCode_whenInactive() {
        // Arrange
        Coupon coupon = new Coupon();
        coupon.setStore(testStore);
        coupon.setCode("INACTIVE10");
        coupon.setDiscountType(Coupon.DiscountType.FLAT);
        coupon.setDiscountValue(new BigDecimal("10.00"));
        coupon.setActive(false);
        coupon.setExpiresAt(OffsetDateTime.now().plusDays(5));
        couponRepository.save(coupon);

        // Act
        Optional<Coupon> found = couponRepository.findActiveByStoreAndCode(testStore.getStoreId(), "INACTIVE10");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should page active coupons for a store")
    void shouldFindAllActiveByStoreId_withPagination() {
        // Arrange
        for (int i = 1; i <= 3; i++) {
            Coupon c = new Coupon();
            c.setStore(testStore);
            c.setCode("PROMO" + i);
            c.setDiscountType(Coupon.DiscountType.FLAT);
            c.setDiscountValue(new BigDecimal("10.00"));
            c.setActive(true);
            couponRepository.save(c);
        }

        // Act
        Page<Coupon> page = couponRepository.findAllActiveByStoreId(testStore.getStoreId(), PageRequest.of(0, 2));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }
}
