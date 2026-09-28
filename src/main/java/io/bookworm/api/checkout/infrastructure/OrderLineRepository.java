package io.bookworm.api.checkout.infrastructure;

import io.bookworm.api.checkout.domain.OrderLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link OrderLine} entities.
 * <p>
 * Why: queries immutable order item snapshots for order detail reconstruction, customer purchase verification,
 * and reporting analytics. Includes specifications and pagination support.
 */
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, UUID>, JpaSpecificationExecutor<OrderLine> {

    /**
     * Finds all active order lines for a specific order.
     */
    @Query("""
           SELECT l FROM OrderLine l
           WHERE l.order.orderId = :orderId
             AND l.deletedAt IS NULL
           ORDER BY l.createdAt ASC
           """)
    List<OrderLine> findActiveByOrderId(@Param("orderId") UUID orderId);

    /**
     * Pages order lines for a specific book across all orders (sales history analytics).
     */
    @Query(value = """
           SELECT l FROM OrderLine l
           WHERE l.book.bookId = :bookId
             AND l.deletedAt IS NULL
           ORDER BY l.createdAt DESC
           """,
           countQuery = """
           SELECT COUNT(l) FROM OrderLine l
           WHERE l.book.bookId = :bookId
             AND l.deletedAt IS NULL
           """)
    Page<OrderLine> findActiveByBookId(@Param("bookId") UUID bookId, Pageable pageable);

    /**
     * Checks if a member has ever purchased a specific book (verified buyer check for reviews).
     */
    @Query("""
           SELECT COUNT(l) > 0 FROM OrderLine l
           WHERE l.order.member.memberId = :memberId
             AND l.book.bookId = :bookId
             AND l.order.status = io.bookworm.api.checkout.domain.Order.OrderStatus.DELIVERED
             AND l.deletedAt IS NULL
           """)
    boolean hasMemberPurchasedBook(
            @Param("memberId") UUID memberId,
            @Param("bookId") UUID bookId);
}
