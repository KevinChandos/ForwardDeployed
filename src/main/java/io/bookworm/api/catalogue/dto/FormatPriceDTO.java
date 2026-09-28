package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Format and current price representation DTO.
 * <p>
 * Why: Transports format edition details (ISBN, page count) and associated active price.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormatPriceDTO {
    private UUID bookFormatId;
    private String formatType;
    private String isbn;
    private Integer pageCount;
    private MoneyDTO price;
}
