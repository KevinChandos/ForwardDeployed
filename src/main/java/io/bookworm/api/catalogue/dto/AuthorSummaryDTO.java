package io.bookworm.api.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Summary representation of an author.
 * <p>
 * Why: Transports author identity, display name, photo URL, book count, and follower count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorSummaryDTO {
    private UUID authorId;
    private String name;
    private String photoUrl;
    private Integer bookCount;
    private Long followerCount;
}
