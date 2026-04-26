package com.sparta.delivery.review.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.sparta.delivery.common.response.PageResponse;
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
import com.sparta.delivery.review.presentation.dto.request.ReviewUpdateRequest;
import com.sparta.delivery.review.presentation.dto.response.ReviewResponse;
import com.sparta.delivery.user.domain.entity.UserRole;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "rating");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public ReviewResponse create(Long actorId, UserRole actorRole, ReviewCreateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(InvalidOrderIdException::new);

        if (!order.getUserId().equals(actorId)) {
            throw new ReviewForbiddenException();
        }

        if(actorRole != UserRole.CUSTOMER) {
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

    public PageResponse<ReviewResponse> getReviews (
            UUID storeId,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = normalizeSize(size);
        String normalizedSortBy = normalizeSortBy(sortBy);

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(parseDirection(direction), normalizedSortBy)
        );
        Page<ReviewResponse> pageResult = reviewRepository.findByStoreId(storeId, pageable)
                .map(ReviewResponse::from);

        return PageResponse.from(pageResult);
    }

    @Transactional
    public ReviewResponse update(Long actorId, UUID reviewId, UserRole actorRole, ReviewUpdateRequest request) {

        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(ReviewNotFoundException::new);
        if (!review.getUserId().equals(actorId)) {
            throw new ReviewForbiddenException();
        }
        if (actorRole != UserRole.CUSTOMER) {
            throw new ReviewForbiddenException();
        }

        review.update(request.rating(), request.content());

        log.info("리뷰 수정 완료 -  actorId={}, reviewId={}", actorId, reviewId);
        return ReviewResponse.from(review);
    }

    @Transactional
    public void delete(Long actorId, UUID reviewId, UserRole actorRole) {

        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        if (!review.getUserId().equals(actorId)) {
            throw new ReviewForbiddenException();
        }
        if (actorRole != UserRole.CUSTOMER) {
            throw new ReviewForbiddenException();
        }

        review.softDelete(actorId);
        log.info("리뷰 삭제 완료 - actorId={}, reviewId={}", actorId, reviewId);

    }

    private Sort.Direction parseDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private int normalizeSize(int size) {
        return ALLOWED_PAGE_SIZES.contains(size) ? size : 10;
    }

    private String normalizeSortBy(String sortBy) {
        return ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
    }

}
