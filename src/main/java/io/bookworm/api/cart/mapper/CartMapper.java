package io.bookworm.api.cart.mapper;

import io.bookworm.api.cart.domain.Cart;
import io.bookworm.api.cart.domain.CartItem;
import io.bookworm.api.cart.dto.CartItemDTO;
import io.bookworm.api.cart.dto.CartResponse;
import io.bookworm.api.catalogue.mapper.CatalogueMapper;
import io.bookworm.api.common.dto.MoneyDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Shopping Cart bounded context.
 * <p>
 * Why: Transforms Cart and CartItem entities into REST response DTOs with computed totals and null safety.
 */
@Mapper(
    componentModel = "spring",
    uses = {CatalogueMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CartMapper {

    @Mapping(target = "book", source = "book")
    @Mapping(target = "bookFormatId", source = "bookFormat.bookFormatId")
    @Mapping(target = "formatType", source = "bookFormat.formatType")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "subtotal", source = "subtotal")
    CartItemDTO toCartItemDTO(CartItem item, MoneyDTO unitPrice, MoneyDTO subtotal);

    @Mapping(target = "cartId", source = "cart.cartId")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "itemCount", source = "itemCount")
    @Mapping(target = "subtotal", source = "subtotal")
    CartResponse toCartResponse(Cart cart, List<CartItemDTO> items, Integer itemCount, MoneyDTO subtotal);
}
