package io.bookworm.api.user.mapper;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.domain.MemberAddress;
import io.bookworm.api.auth.mapper.AuthMapper;
import io.bookworm.api.catalogue.mapper.CatalogueMapper;
import io.bookworm.api.user.domain.WishlistItem;
import io.bookworm.api.user.dto.*;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for User & Profile bounded context.
 * <p>
 * Why: Maps member addresses, profiles, and wishlist entities with complete null-safety.
 */
@Mapper(
    componentModel = "spring",
    uses = {AuthMapper.class, CatalogueMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "addressId", source = "addressId")
    @Mapping(target = "isDefault", source = "default")
    MemberAddressDTO toAddressDTO(MemberAddress address);

    List<MemberAddressDTO> toAddressDTOList(List<MemberAddress> addresses);

    @Mapping(target = "addressId", ignore = true)
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "default", source = "isDefault")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    MemberAddress toAddressEntity(CreateAddressRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "addressId", ignore = true)
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "default", source = "isDefault")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateAddressEntityFromDTO(UpdateAddressRequest request, @MappingTarget MemberAddress address);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "memberId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateMemberEntityFromDTO(UpdateProfileRequest request, @MappingTarget Member member);

    @Mapping(target = "member", source = "member")
    @Mapping(target = "phoneNumber", source = "member.phoneNumber")
    @Mapping(target = "addresses", source = "addresses")
    MemberProfileResponse toProfileResponse(Member member, List<MemberAddress> addresses);

    @Mapping(target = "book", source = "book")
    @Mapping(target = "bookFormatId", source = "bookFormat.bookFormatId")
    @Mapping(target = "formatType", source = "bookFormat.formatType")
    @Mapping(target = "addedAt", source = "addedAt")
    WishlistItemDTO toWishlistItemDTO(WishlistItem item);

    List<WishlistItemDTO> toWishlistItemDTOList(List<WishlistItem> items);
}
