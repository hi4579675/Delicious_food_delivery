package com.sparta.delivery.review.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sparta.delivery.review.domain.exception.InvalidContentException;
import com.sparta.delivery.review.domain.exception.InvalidRatingException;
import com.sparta.delivery.review.domain.exception.ReviewErrorCode;

class ReviewTest {

    @Test
    @DisplayName("create: 정상 입력이면 Review를 생성한다")
    void create_success() {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.create(orderId, storeId, userId, 5, "  맛있어요  ");

        assertThat(review.getOrderId()).isEqualTo(orderId);
        assertThat(review.getStoreId()).isEqualTo(storeId);
        assertThat(review.getUserId()).isEqualTo(userId);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("맛있어요");
    }

    @Test
    @DisplayName("create: content가 null이면 null로 저장된다")
    void create_success_whenContentNull() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 4, null);

        assertThat(review.getContent()).isNull();
    }

    @Test
    @DisplayName("create: content가 공백이면 null로 정규화된다")
    void create_success_whenContentBlank() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 4, "   ");

        assertThat(review.getContent()).isNull();
    }

    @Test
    @DisplayName("create: rating이 null이면 예외가 발생한다")
    void create_fail_whenRatingNull() {
        Throwable thrown = catchThrowable(
                () -> Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, null, "good")
        );

        assertThat(thrown).isInstanceOf(InvalidRatingException.class);
        InvalidRatingException exception = (InvalidRatingException) thrown;
        assertThat(exception.getCode()).isEqualTo(ReviewErrorCode.INVALID_RATING.getCode());
    }

    @Test
    @DisplayName("create: rating이 1 미만이면 예외가 발생한다")
    void create_fail_whenRatingLessThanOne() {
        Throwable thrown = catchThrowable(
                () -> Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 0, "good")
        );

        assertThat(thrown).isInstanceOf(InvalidRatingException.class);
    }

    @Test
    @DisplayName("create: rating이 5 초과면 예외가 발생한다")
    void create_fail_whenRatingGreaterThanFive() {
        Throwable thrown = catchThrowable(
                () -> Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 6, "good")
        );

        assertThat(thrown).isInstanceOf(InvalidRatingException.class);
    }

    @Test
    @DisplayName("create: content가 500자를 초과하면 예외가 발생한다")
    void create_fail_whenContentTooLong() {
        String longContent = "a".repeat(501);

        Throwable thrown = catchThrowable(
                () -> Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 5, longContent)
        );

        assertThat(thrown).isInstanceOf(InvalidContentException.class);
        InvalidContentException exception = (InvalidContentException) thrown;
        assertThat(exception.getCode()).isEqualTo(ReviewErrorCode.INVALID_CONTENT.getCode());
    }

    @Test
    @DisplayName("update: 정상 입력이면 rating/content가 갱신된다")
    void update_success() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 3, "good");

        review.update(5, "  excellent  ");

        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("excellent");
    }

    @Test
    @DisplayName("update: content가 공백이면 null로 정규화된다")
    void update_success_whenContentBlank() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 3, "good");

        review.update(4, "   ");

        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getContent()).isNull();
    }

    @Test
    @DisplayName("update: 유효하지 않은 rating이면 값이 변경되지 않는다")
    void update_fail_whenInvalidRating_thenStateUnchanged() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 3, "good");

        Throwable thrown = catchThrowable(() -> review.update(0, "new content"));

        assertThat(thrown).isInstanceOf(InvalidRatingException.class);
        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getContent()).isEqualTo("good");
    }

    @Test
    @DisplayName("update: 유효하지 않은 content면 값이 변경되지 않는다")
    void update_fail_whenInvalidContent_thenStateUnchanged() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 1L, 3, "good");

        Throwable thrown = catchThrowable(() -> review.update(5, "a".repeat(501)));

        assertThat(thrown).isInstanceOf(InvalidContentException.class);
        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getContent()).isEqualTo("good");
    }
}
