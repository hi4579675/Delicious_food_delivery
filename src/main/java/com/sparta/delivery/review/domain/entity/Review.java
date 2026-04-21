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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.sparta.delivery.common.model.BaseEntity;

@Entity
@Table(name = "p_review", uniqueConstraints = {@UniqueConstraint(name = "uk_review_order_id", columnNames = "order_id")}, indexes = {@Index(name = "idx_review_store_id", columnList = "store_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Builder
    private Review(UUID orderId, UUID storeId, Long userId, Integer rating, String content) {
        this.orderId = orderId;
        this.storeId = storeId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
    }

    @Id
    @Column(name = "review_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID storeId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content")
    private String content;

    public void update(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
    }


}
