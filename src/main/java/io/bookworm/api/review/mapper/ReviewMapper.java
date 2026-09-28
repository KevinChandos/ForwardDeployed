package io.bookworm.api.review.mapper;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.review.domain.Review;
import io.bookworm.api.review.dto.ModerationReviewDTO;
import io.bookworm.api.review.dto.ReviewDTO;
import io.bookworm.api.review.dto.SubmitReviewRequest;
import io.bookworm.api.review.dto.UpdateReviewRequest;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper for Review bounded context.
 * <p>
 * Why: Converts Review domain entities into public ReviewDTO and moderation ModerationReviewDTO objects.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReviewMapper {

    @Mapping(target = "reviewId", source = "review.reviewId")
    @Mapping(target = "memberName", source = "review.member", qualifiedByName = "formatMemberName")
    @Mapping(target = "rating", source = "review.rating")
    @Mapping(target = "body", source = "review.body")
    @Mapping(target = "status", source = "review.status")
    @Mapping(target = "publishedAt", source = "review.publishedAt")
    @Mapping(target = "submittedAt", source = "review.submittedAt")
    @Mapping(target = "isOwn", source = "isOwn")
    ReviewDTO toReviewDTO(Review review, Boolean isOwn);

    default ReviewDTO toPublicReviewDTO(Review review, UUID currentMemberId) {
        if (review == null) {
            return null;
        }
        boolean isOwn = currentMemberId != null
                && review.getMember() != null
                && currentMemberId.equals(review.getMember().getMemberId());
        return toReviewDTO(review, isOwn);
    }

    @Mapping(target = "reviewId", source = "review.reviewId")
    @Mapping(target = "bookId", source = "review.book.bookId")
    @Mapping(target = "bookTitle", source = "review.book.title")
    @Mapping(target = "memberId", source = "review.member.memberId")
    @Mapping(target = "memberName", source = "review.member", qualifiedByName = "formatMemberName")
    @Mapping(target = "rating", source = "review.rating")
    @Mapping(target = "body", source = "review.body")
    @Mapping(target = "submittedAt", source = "review.submittedAt")
    ModerationReviewDTO toModerationReviewDTO(Review review);

    List<ModerationReviewDTO> toModerationReviewDTOList(List<Review> reviews);

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "submittedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Review toReviewEntity(SubmitReviewRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "submittedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateReviewEntityFromDTO(UpdateReviewRequest request, @MappingTarget Review review);

    @Named("formatMemberName")
    default String formatMemberName(Member member) {
        if (member == null) {
            return "Anonymous";
        }
        String first = member.getFirstName() != null ? member.getFirstName() : "";
        String last = member.getLastName() != null ? member.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "Anonymous" : name;
    }
}
