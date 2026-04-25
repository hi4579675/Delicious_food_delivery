package com.sparta.delivery.review.domain.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.sparta.delivery.common.model.BaseEntity;
import com.sparta.delivery.review.domain.exception.InvalidContentException;
import com.sparta.delivery.review.domain.exception.InvalidRatingException;

@Entity
@Table(name = "p_review", uniqueConstraints = {@UniqueConstraint(name = "uk_review_order_id", columnNames = "order_id")}, indexes = {@Index(name = "idx_review_store_id", columnList = "store_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Review extends BaseEntity {

    @Builder(access = AccessLevel.PRIVATE)
    private Review(UUID orderId, UUID storeId, Long userId, Integer rating, String content) {
        this.orderId = orderId;
        this.storeId = storeId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
    }

    public static Review create(
            UUID orderId,
            UUID storeId,
            Long userId,
            Integer rating,
            String content
    ) {
        String normalizedContent = normalizeOptional(content);
        validate(rating, content);
        return Review.builder()
                .orderId(orderId)
                .storeId(storeId)
                .userId(userId)
                .rating(rating)
                .content(normalizedContent)
                .build();
    }

    private static void validate(Integer rating, String content) {
        validateRating(rating);
        validateContent(content);
    }

    private static void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new InvalidRatingException();
        }
    }

    private static void validateContent(String content) {
        String value = normalizeOptional(content);

        if (value != null && value.length() > 500) {
            throw new InvalidContentException();
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

    @Column(nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false, updatable = false)
    private UUID storeId;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length=500)
    private String content;

    public void update(Integer rating, String content) {

        String normalizedContent = normalizeOptional(content);
        validate(rating, content);
        this.rating = rating;
        this.content = normalizedContent;

    }

}
