package io.bookworm.api.catalogue.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating a new book.
 * <p>
 * Why: Accepts full book metadata, author credits, publisher, categories, and initial formats with prices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    private String title;

    private String synopsis;

    @NotBlank(message = "Language is required")
    @Size(max = 50, message = "Language cannot exceed 50 characters")
    private String language;

    @Size(max = 2000, message = "Cover image URL cannot exceed 2000 characters")
    private String coverImageUrl;

    private LocalDate publishedDate;

    private UUID publisherId;

    @NotEmpty(message = "At least one author must be credited")
    @Valid
    private List<AuthorRoleInput> authors;

    @NotEmpty(message = "At least one category must be assigned")
    private List<UUID> categoryIds;

    @NotEmpty(message = "At least one format edition must be defined")
    @Valid
    private List<CreateFormatDTO> formats;
}
