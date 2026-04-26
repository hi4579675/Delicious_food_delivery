package com.sparta.delivery.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderItem;
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

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Nested
    @DisplayName("리뷰 생성")
    class Create {

        @Test
        @DisplayName("CUSTOMER 본인 주문이 완료 상태면 리뷰를 생성한다")
        void create_success() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, actorId, OrderStatus.COMPLETED);
            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "  맛있어요  ");

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(reviewRepository.existsAnyByOrderIdIncludingDeleted(orderId)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
                Review review = invocation.getArgument(0);
                ReflectionTestUtils.setField(review, "reviewId", UUID.randomUUID());
                return review;
            });

            // when
            ReviewResponse response = reviewService.create(actorId, UserRole.CUSTOMER, request);

            // then
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.storeId()).isEqualTo(order.getStoreId());
            assertThat(response.userId()).isEqualTo(actorId);
            assertThat(response.rating()).isEqualTo(5);
            assertThat(response.content()).isEqualTo("맛있어요");
            then(reviewRepository).should().save(any(Review.class));
        }

        @Test
        @DisplayName("주문이 없으면 InvalidOrderIdException")
        void create_fail_whenOrderNotFound() {
            // given
            UUID orderId = UUID.randomUUID();
            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "좋아요");
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.create(1L, UserRole.CUSTOMER, request))
                    .isInstanceOf(InvalidOrderIdException.class);
            then(reviewRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("본인 주문이 아니면 ReviewForbiddenException")
        void create_fail_whenNotOwner() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, OrderStatus.COMPLETED);
            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "좋아요");
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> reviewService.create(actorId, UserRole.CUSTOMER, request))
                    .isInstanceOf(ReviewForbiddenException.class);
            then(reviewRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("CUSTOMER가 아니면 ReviewForbiddenException")
        void create_fail_whenRoleIsNotCustomer() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, actorId, OrderStatus.COMPLETED);
            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "좋아요");
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> reviewService.create(actorId, UserRole.OWNER, request))
                    .isInstanceOf(ReviewForbiddenException.class);
            then(reviewRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("주문이 완료 상태가 아니면 InvalidOrderStatusException")
        void create_fail_whenOrderNotCompleted() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, actorId, OrderStatus.DELIVERED);
            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "좋아요");
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> reviewService.create(actorId, UserRole.CUSTOMER, request))
                    .isInstanceOf(InvalidOrderStatusException.class);
            then(reviewRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 리뷰가 존재하면 DuplicateReviewOrderException")
        void create_fail_whenDuplicateExists() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, actorId, OrderStatus.COMPLETED);
            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "좋아요");

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(reviewRepository.existsAnyByOrderIdIncludingDeleted(orderId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.create(actorId, UserRole.CUSTOMER, request))
                    .isInstanceOf(DuplicateReviewOrderException.class);
            then(reviewRepository).should().existsAnyByOrderIdIncludingDeleted(orderId);
        }

        @Test
        @DisplayName("저장 중 무결성 예외가 발생하면 DuplicateReviewOrderException")
        void create_fail_whenDataIntegrityViolation() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, actorId, OrderStatus.COMPLETED);
            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "좋아요");

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(reviewRepository.existsAnyByOrderIdIncludingDeleted(orderId)).willReturn(false);
            given(reviewRepository.save(any(Review.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate"));

            // when & then
            assertThatThrownBy(() -> reviewService.create(actorId, UserRole.CUSTOMER, request))
                    .isInstanceOf(DuplicateReviewOrderException.class);
        }
    }

    @Nested
    @DisplayName("리뷰 단건 조회")
    class GetByReviewId {

        @Test
        @DisplayName("리뷰 ID로 단건 조회한다")
        void getByReviewId_success() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Review review = createReview(reviewId, orderId, storeId, 1L, 4, "good");
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when
            ReviewResponse response = reviewService.getByReviewId(reviewId);

            // then
            assertThat(response.reviewId()).isEqualTo(reviewId);
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.storeId()).isEqualTo(storeId);
            assertThat(response.rating()).isEqualTo(4);
        }

        @Test
        @DisplayName("리뷰가 없으면 ReviewNotFoundException")
        void getByReviewId_fail_whenNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getByReviewId(reviewId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("리뷰 목록 조회")
    class GetReviews {

        @Test
        @DisplayName("허용되지 않은 size/sortBy는 기본값으로 보정된다")
        void getReviews_success_withNormalization() {
            // given
            UUID storeId = UUID.randomUUID();
            Review review = createReview(UUID.randomUUID(), UUID.randomUUID(), storeId, 1L, 5, "great");

            given(reviewRepository.findByStoreIdAndDeletedAtIsNull(any(UUID.class), any(Pageable.class)))
                    .willReturn(new PageImpl<>(
                            List.of(review),
                            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                            1
                    ));

            // when
            PageResponse<ReviewResponse> response = reviewService.getReviews(storeId, -1, 7, "unknown", "invalid");

            // then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            then(reviewRepository).should().findByStoreIdAndDeletedAtIsNull(any(UUID.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);

            assertThat(response.content()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("허용된 sortBy/direction/size를 그대로 사용한다")
        void getReviews_success_withRequestedSorting() {
            // given
            UUID storeId = UUID.randomUUID();
            given(reviewRepository.findByStoreIdAndDeletedAtIsNull(any(UUID.class), any(Pageable.class)))
                    .willReturn(Page.empty());

            // when
            reviewService.getReviews(storeId, 2, 30, "rating", "asc");

            // then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            then(reviewRepository).should().findByStoreIdAndDeletedAtIsNull(any(UUID.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(30);
            assertThat(pageable.getSort().getOrderFor("rating")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("rating").getDirection()).isEqualTo(Sort.Direction.ASC);
        }
    }

    @Nested
    @DisplayName("리뷰 수정")
    class Update {

        @Test
        @DisplayName("작성자인 CUSTOMER는 리뷰를 수정할 수 있다")
        void update_success() {
            // given
            Long actorId = 1L;
            UUID reviewId = UUID.randomUUID();
            Review review = createReview(reviewId, UUID.randomUUID(), UUID.randomUUID(), actorId, 3, "old");
            ReviewUpdateRequest request = new ReviewUpdateRequest(5, "  new content  ");

            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.of(review));

            // when
            ReviewResponse response = reviewService.update(actorId, reviewId, UserRole.CUSTOMER, request);

            // then
            assertThat(response.rating()).isEqualTo(5);
            assertThat(response.content()).isEqualTo("new content");
        }

        @Test
        @DisplayName("리뷰가 없으면 ReviewNotFoundException")
        void update_fail_whenNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            ReviewUpdateRequest request = new ReviewUpdateRequest(5, "new");
            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.update(1L, reviewId, UserRole.CUSTOMER, request))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아니면 ReviewForbiddenException")
        void update_fail_whenNotOwner() {
            // given
            UUID reviewId = UUID.randomUUID();
            Review review = createReview(reviewId, UUID.randomUUID(), UUID.randomUUID(), 2L, 3, "old");
            ReviewUpdateRequest request = new ReviewUpdateRequest(5, "new");
            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.update(1L, reviewId, UserRole.CUSTOMER, request))
                    .isInstanceOf(ReviewForbiddenException.class);
        }

        @Test
        @DisplayName("CUSTOMER가 아니면 ReviewForbiddenException")
        void update_fail_whenRoleNotCustomer() {
            // given
            Long actorId = 1L;
            UUID reviewId = UUID.randomUUID();
            Review review = createReview(reviewId, UUID.randomUUID(), UUID.randomUUID(), actorId, 3, "old");
            ReviewUpdateRequest request = new ReviewUpdateRequest(5, "new");
            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.update(actorId, reviewId, UserRole.OWNER, request))
                    .isInstanceOf(ReviewForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("리뷰 삭제")
    class Delete {

        @Test
        @DisplayName("작성자인 CUSTOMER는 리뷰를 삭제할 수 있다")
        void delete_success() {
            // given
            Long actorId = 1L;
            UUID reviewId = UUID.randomUUID();
            Review review = createReview(reviewId, UUID.randomUUID(), UUID.randomUUID(), actorId, 4, "good");
            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.of(review));

            // when
            reviewService.delete(actorId, reviewId, UserRole.CUSTOMER);

            // then
            assertThat(review.isDeleted()).isTrue();
            assertThat(review.getDeletedBy()).isEqualTo(actorId);
        }

        @Test
        @DisplayName("리뷰가 없으면 ReviewNotFoundException")
        void delete_fail_whenNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.delete(1L, reviewId, UserRole.CUSTOMER))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아니면 ReviewForbiddenException")
        void delete_fail_whenNotOwner() {
            // given
            UUID reviewId = UUID.randomUUID();
            Review review = createReview(reviewId, UUID.randomUUID(), UUID.randomUUID(), 2L, 4, "good");
            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.delete(1L, reviewId, UserRole.CUSTOMER))
                    .isInstanceOf(ReviewForbiddenException.class);
        }

        @Test
        @DisplayName("CUSTOMER가 아니면 ReviewForbiddenException")
        void delete_fail_whenRoleNotCustomer() {
            // given
            Long actorId = 1L;
            UUID reviewId = UUID.randomUUID();
            Review review = createReview(reviewId, UUID.randomUUID(), UUID.randomUUID(), actorId, 4, "good");
            given(reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.delete(actorId, reviewId, UserRole.MANAGER))
                    .isInstanceOf(ReviewForbiddenException.class);
        }
    }

    private Order createOrder(UUID orderId, Long userId, OrderStatus status) {
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        OrderItem item = OrderItem.create(UUID.randomUUID(), 1, 12_000, "치킨");

        Order order = Order.create(storeId, addressId, userId, "서울시 중구 세종대로 1", List.of(item));
        ReflectionTestUtils.setField(order, "orderId", orderId);

        switch (status) {
            case PENDING -> {
            }
            case ACCEPTED -> order.accept();
            case COOKING -> {
                order.accept();
                order.startCooking();
            }
            case DELIVERING -> {
                order.accept();
                order.startCooking();
                order.startDelivery();
            }
            case DELIVERED -> {
                order.accept();
                order.startCooking();
                order.startDelivery();
                order.markDelivered();
            }
            case COMPLETED -> {
                order.accept();
                order.startCooking();
                order.startDelivery();
                order.markDelivered();
                order.complete();
            }
            case REJECTED -> order.reject();
            case CANCELED -> order.cancel();
        }

        return order;
    }

    private Review createReview(
            UUID reviewId,
            UUID orderId,
            UUID storeId,
            Long userId,
            int rating,
            String content
    ) {
        Review review = Review.create(orderId, storeId, userId, rating, content);
        ReflectionTestUtils.setField(review, "reviewId", reviewId);
        return review;
    }
}
