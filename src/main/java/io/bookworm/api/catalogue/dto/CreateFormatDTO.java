package io.bookworm.api.catalogue.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Format edition creation DTO for book creation.
 * <p>
 * Why: Encapsulates format type (PAPERBACK, HARDCOVER, EBOOK), ISBN, page count, and store prices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFormatDTO {

    @NotNull(message = "Format type is required")
    private String formatType;

    @Size(max = 20, message = "ISBN cannot exceed 20 characters")
    private String isbn;

    private Integer pageCount;

    @NotEmpty(message = "At least one price is required for the format")
    @Valid
    private List<CreatePriceDTO> prices;
}
