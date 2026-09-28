package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight publisher reference DTO.
 * <p>
 * Why: Transports publisher identity and name for book summary and detail views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherRefDTO {
    private UUID publisherId;
    private String name;
}
