package com.sparta.delivery.review.application.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import com.sparta.delivery.order.domain.repository.OrderRepository;
import com.sparta.delivery.review.domain.entity.Review;
import com.sparta.delivery.review.domain.exception.DuplicateReviewOrderException;
import com.sparta.delivery.review.domain.exception.InvalidOrderIdException;
import com.sparta.delivery.review.domain.exception.InvalidOrderStatusException;
import com.sparta.delivery.review.domain.exception.ReviewForbiddenException;
import com.sparta.delivery.review.domain.exception.ReviewNotFoundException;
import com.sparta.delivery.review.domain.repository.ReviewRepository;
import com.sparta.delivery.review.presentation.dto.request.ReviewCreateRequest;
import com.sparta.delivery.review.presentation.dto.response.ReviewResponse;
import com.sparta.delivery.user.domain.entity.UserRole;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public ReviewResponse create(Long actorId, UserRole actorRole, ReviewCreateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(InvalidOrderIdException::new);

        if (!order.getUserId().equals(actorId)) {
            throw new ReviewForbiddenException();
        }

        if (actorRole != UserRole.CUSTOMER) {
            throw new ReviewForbiddenException();
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new InvalidOrderStatusException();
        }

        if (reviewRepository.existsAnyByOrderIdIncludingDeleted(order.getOrderId())) {
            throw new DuplicateReviewOrderException();
        }

        Review saved = Review.create(order.getOrderId(), order.getStoreId(), actorId, request.rating(),
                request.content());

        try {
            log.info("리뷰 생성 완료 - actorId={}, orderId={}", actorId, order.getOrderId());
            return ReviewResponse.from(reviewRepository.save(saved));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateReviewOrderException();
        }
    }

    public ReviewResponse getByReviewId(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        return ReviewResponse.from(review);
    }

}
