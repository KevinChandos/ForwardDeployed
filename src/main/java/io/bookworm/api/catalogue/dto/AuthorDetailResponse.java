package io.bookworm.api.catalogue.dto;

import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Detailed author response representation.
 * <p>
 * Why: Encapsulates complete author biography, photo, followers, and their books.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDetailResponse {
    private UUID authorId;
    private String name;
    private String bio;
    private String photoUrl;
    private Long followerCount;
    private Boolean isFollowing;
    private List<BookSummaryDTO> books;
    private PaginationDTO pagination;
}
