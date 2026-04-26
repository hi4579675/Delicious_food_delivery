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

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    Page<Review> findByStoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    Optional<Review> findByReviewIdAndDeletedAtIsNull(UUID reviewId);

    @Query(value = "select exists (select 1 from p_review where order_id = :orderId)", nativeQuery = true)
    boolean existsAnyByOrderIdIncludingDeleted(@Param("orderId") UUID orderId);
}
