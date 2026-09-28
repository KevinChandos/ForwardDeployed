package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Publisher representation DTO.
 * <p>
 * Why: Transports publisher identity, name, website, and active status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherDTO {
    private UUID publisherId;
    private String name;
    private String website;
    private Boolean isActive;
}
