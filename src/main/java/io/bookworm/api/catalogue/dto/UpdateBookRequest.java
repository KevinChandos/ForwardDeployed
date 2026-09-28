package io.bookworm.api.catalogue.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for updating book metadata.
 * <p>
 * Why: Allows partial modification of book title, synopsis, language, cover photo, publisher, and publication date.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookRequest {

    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    private String title;

    private String synopsis;

    @Size(min = 1, max = 50, message = "Language must be between 1 and 50 characters")
    private String language;

    @Size(max = 2000, message = "Cover image URL cannot exceed 2000 characters")
    private String coverImageUrl;

    private LocalDate publishedDate;

    private UUID publisherId;
}
