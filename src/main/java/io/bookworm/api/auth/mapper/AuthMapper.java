package io.bookworm.api.auth.mapper;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.domain.MemberRole;
import io.bookworm.api.auth.dto.MemberSummaryDTO;
import io.bookworm.api.auth.dto.RegisterResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for authentication domain entities and DTOs.
 * <p>
 * Why: Converts Member entities to security summaries and registration responses with null safety.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuthMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    MemberSummaryDTO toMemberSummaryDTO(Member member);

    @Mapping(target = "message", constant = "Account created successfully")
    RegisterResponse toRegisterResponse(Member member);

    @Named("mapRolesToStrings")
    default List<String> mapRolesToStrings(List<MemberRole> roles) {
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream()
                .filter(r -> r.getDeletedAt() == null && r.getRoleName() != null)
                .map(r -> r.getRoleName().name())
                .toList();
    }
}
