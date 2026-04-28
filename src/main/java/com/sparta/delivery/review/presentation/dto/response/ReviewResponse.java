package com.sparta.delivery.review.presentation.dto.response;

import java.util.UUID;

import com.sparta.delivery.review.domain.entity.Review;

public record ReviewResponse(

        UUID reviewId,
        UUID orderId,
        UUID storeId,
        Long userId,
        Integer rating,
        String content

) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getOrderId(),
                review.getStoreId(),
                review.getUserId(),
                review.getRating(),
                review.getContent()
        );
    }
}
