package com.sparta.delivery.review.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sparta.delivery.review.domain.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    interface StoreRatingSummary {
        Long getRatingSum();
        Long getReviewCount();
    }

    Page<Review> findByStoreId(UUID storeId, Pageable pageable);

    Optional<Review> findByReviewId(UUID reviewId);

    @Query("""
            select coalesce(sum(r.rating), 0) as ratingSum,
                   count(r) as reviewCount
            from Review r
            where r.storeId = :storeId
            """)
    StoreRatingSummary getStoreRatingSummary(@Param("storeId") UUID storeId);

    @Query(value = "select exists (select 1 from p_review where order_id = :orderId)", nativeQuery = true)
    boolean existsAnyByOrderIdIncludingDeleted(@Param("orderId") UUID orderId);
}
