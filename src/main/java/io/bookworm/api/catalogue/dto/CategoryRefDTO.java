package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight category reference DTO.
 * <p>
 * Why: Transports category identifier, name, and URL slug.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRefDTO {
    private UUID categoryId;
    private String name;
    private String slug;
}
