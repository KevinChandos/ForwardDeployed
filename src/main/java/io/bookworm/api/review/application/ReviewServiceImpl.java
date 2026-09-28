package io.bookworm.api.review.application;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.catalogue.domain.Book;
import io.bookworm.api.catalogue.infrastructure.BookRepository;
import io.bookworm.api.checkout.infrastructure.OrderLineRepository;
import io.bookworm.api.common.dto.PaginationDTO;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceConflictException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.review.domain.Review;
import io.bookworm.api.review.dto.*;
import io.bookworm.api.review.infrastructure.ReviewRepository;
import io.bookworm.api.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Review & Rating bounded context.
 * <p>
 * Why: Orchestrates review submission, verification that user is authenticated, rating bounds checks (1–5 stars),
 * duplicate review rejection, book rating aggregates recomputation, and moderator queue operations.
 * Side effects: Mutates Review and Book aggregate rating statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final OrderLineRepository orderLineRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public ReviewListResponse getBookReviews(UUID bookId, UUID currentMemberId, Pageable pageable) {
        log.info("Fetching published reviews for bookId: {}", bookId);
        Page<Review> page = reviewRepository.findPublishedByBookId(bookId, pageable);

        List<ReviewDTO> reviewDTOs = page.getContent().stream()
                .map(r -> reviewMapper.toPublicReviewDTO(r, currentMemberId))
                .toList();

        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return ReviewListResponse.builder()
                .content(reviewDTOs)
                .pagination(pagination)
                .build();
    }

    @Override
    public ReviewDTO getMyReview(UUID bookId, UUID memberId) {
        log.info("Fetching review by member: {} for book: {}", memberId, bookId);
        Review review = reviewRepository.findByBookAndMember(bookId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Review for book", bookId));

        return reviewMapper.toPublicReviewDTO(review, memberId);
    }

    @Override
    @Transactional
    public ReviewDTO submitReview(UUID bookId, UUID memberId, SubmitReviewRequest request) {
        log.info("Submitting review for bookId: {} by memberId: {}", bookId, memberId);

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BusinessRuleException("INVALID_RATING", "Star rating must be between 1 and 5");
        }

        if (reviewRepository.findByBookAndMember(bookId, memberId).isPresent()) {
            throw new ResourceConflictException("REVIEW_EXISTS", "You have already reviewed this book");
        }

        Book book = bookRepository.findActiveWithDetails(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        Review review = reviewMapper.toReviewEntity(request);
        review.setBook(book);
        review.setMember(member);
        review.setStatus(Review.ReviewStatus.PENDING);
        review.setSubmittedAt(OffsetDateTime.now());

        Review saved = reviewRepository.save(review);
        log.info("Review {} submitted for moderation", saved.getReviewId());

        return reviewMapper.toPublicReviewDTO(saved, memberId);
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(UUID bookId, UUID memberId, UpdateReviewRequest request) {
        log.info("Updating review for bookId: {} by memberId: {}", bookId, memberId);
        Review review = reviewRepository.findByBookAndMember(bookId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Review for book", bookId));

        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new BusinessRuleException("INVALID_RATING", "Star rating must be between 1 and 5");
        }

        reviewMapper.updateReviewEntityFromDTO(request, review);
        review.setStatus(Review.ReviewStatus.PENDING); // Requires re-moderation on update
        review.setSubmittedAt(OffsetDateTime.now());

        Review saved = reviewRepository.save(review);
        return reviewMapper.toPublicReviewDTO(saved, memberId);
    }

    @Override
    @Transactional
    public void deleteReview(UUID bookId, UUID memberId) {
        log.info("Deleting review for bookId: {} by memberId: {}", bookId, memberId);
        Review review = reviewRepository.findByBookAndMember(bookId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Review for book", bookId));

        review.setDeletedAt(OffsetDateTime.now());
        reviewRepository.save(review);
        log.info("Review {} soft-deleted", review.getReviewId());
    }

    @Override
    public ModerationReviewListResponse getPendingReviews(Pageable pageable) {
        log.info("Fetching pending moderation queue");
        Page<Review> page = reviewRepository.findPendingModeration(pageable);
        List<ModerationReviewDTO> items = reviewMapper.toModerationReviewDTOList(page.getContent());

        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return ModerationReviewListResponse.builder()
                .content(items)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public void approveReview(UUID reviewId) {
        log.info("Approving review: {}", reviewId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        review.setStatus(Review.ReviewStatus.PUBLISHED);
        review.setPublishedAt(OffsetDateTime.now());
        review.setRejectionReason(null);
        reviewRepository.save(review);

        // Update book aggregate rating
        Book book = review.getBook();
        int currentCount = book.getReviewCount() != null ? book.getReviewCount() : 0;
        double currentAvg = book.getAverageRating() != null ? book.getAverageRating() : 0.0;
        double newAvg = ((currentAvg * currentCount) + review.getRating()) / (currentCount + 1);

        book.setReviewCount(currentCount + 1);
        book.setAverageRating(Math.round(newAvg * 10.0) / 10.0);
        bookRepository.save(book);
        log.info("Review {} published; book rating updated", reviewId);
    }

    @Override
    @Transactional
    public void rejectReview(UUID reviewId, RejectReviewRequest request) {
        log.info("Rejecting review: {}", reviewId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        review.setStatus(Review.ReviewStatus.REJECTED);
        review.setRejectionReason(request.getReason() != null ? request.getReason() : "Does not meet community guidelines");
        reviewRepository.save(review);
        log.info("Review {} marked REJECTED", reviewId);
    }
}
