package io.bookworm.api.promotions.mapper;

import io.bookworm.api.catalogue.mapper.CatalogueMapper;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.promotions.domain.Coupon;
import io.bookworm.api.promotions.dto.CouponDTO;
import io.bookworm.api.promotions.dto.CreateCouponRequest;
import io.bookworm.api.promotions.dto.UpdateCouponRequest;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for Promotions & Coupons bounded context.
 * <p>
 * Why: Converts Coupon entities into CouponDTOs and maps creation/update request payloads with null safety.
 */
@Mapper(
    componentModel = "spring",
    uses = {CatalogueMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PromotionsMapper {

    @Mapping(target = "couponId", source = "coupon.couponId")
    @Mapping(target = "code", source = "coupon.code")
    @Mapping(target = "description", source = "coupon.description")
    @Mapping(target = "discountType", source = "coupon.discountType")
    @Mapping(target = "discountValue", source = "coupon.discountValue", qualifiedByName = "bigDecimalToDouble")
    @Mapping(target = "minOrderAmount", source = "coupon.minOrderAmount", qualifiedByName = "amountToMoneyDTO")
    @Mapping(target = "maxUses", source = "coupon.maxUses")
    @Mapping(target = "usedCount", source = "coupon.usedCount")
    @Mapping(target = "expiresAt", source = "coupon.expiresAt")
    @Mapping(target = "isActive", source = "coupon.active")
    CouponDTO toCouponDTO(Coupon coupon);

    List<CouponDTO> toCouponDTOList(List<Coupon> coupons);

    @Mapping(target = "couponId", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "usedCount", constant = "0")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "redemptions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Coupon toCouponEntity(CreateCouponRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "couponId", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "discountType", ignore = true)
    @Mapping(target = "discountValue", ignore = true)
    @Mapping(target = "minOrderAmount", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "redemptions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateCouponEntityFromDTO(UpdateCouponRequest request, @MappingTarget Coupon coupon);

    @Named("bigDecimalToDouble")
    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    @Named("amountToMoneyDTO")
    default MoneyDTO amountToMoneyDTO(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return MoneyDTO.builder()
                .amount(amount.toPlainString())
                .currency("INR")
                .build();
    }
}
