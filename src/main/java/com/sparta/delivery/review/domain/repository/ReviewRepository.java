package com.sparta.delivery.review.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sparta.delivery.review.domain.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    Page<Review> findByStoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    Optional<Review> findByReviewIdAndDeletedAtIsNull(UUID reviewId);

}
