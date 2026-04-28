package com.sparta.delivery.store.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import com.sparta.delivery.store.domain.exception.InvalidCategoryIdException;
import com.sparta.delivery.store.domain.exception.InvalidMinOrderAmountException;
import com.sparta.delivery.store.domain.exception.InvalidRegionIdException;
import com.sparta.delivery.store.domain.exception.InvalidReviewCountException;
import com.sparta.delivery.store.domain.exception.InvalidStoreActiveStatusException;
import com.sparta.delivery.store.domain.exception.InvalidStoreAddressException;
import com.sparta.delivery.store.domain.exception.InvalidStoreNameException;
import com.sparta.delivery.store.domain.exception.InvalidStoreOpenStatusException;
import com.sparta.delivery.store.domain.exception.InvalidUserIdException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "p_store")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id", updatable = false, nullable = false)
    private UUID storeId;

    @Column(name = "region_id", nullable = false)
    private UUID regionId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "description")
    private String description;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "min_order_amount", nullable = false)
    private Integer minOrderAmount;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "avg_rating", precision = 2, scale = 1)
    private BigDecimal avgRating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Builder(access = AccessLevel.PRIVATE)
    private Store(
            UUID regionId,
            UUID categoryId,
            Long userId,
            String storeName,
            String description,
            String address,
            String addressDetail,
            String phoneNumber,
            Integer minOrderAmount,
            Boolean isOpen,
            Boolean isActive,
            BigDecimal avgRating,
            Integer reviewCount
    ) {
        String normalizedStoreName = normalize(storeName);
        String normalizedDescription = normalize(description);
        String normalizedAddress = normalize(address);
        String normalizedAddressDetail = normalize(addressDetail);
        String normalizedPhoneNumber = normalize(phoneNumber);

        validateRegionId(regionId);
        validateCategoryId(categoryId);
        validateUserId(userId);
        validateStoreName(normalizedStoreName);
        validateAddress(normalizedAddress);
        validateMinOrderAmount(minOrderAmount);
        validateIsOpen(isOpen);
        validateIsActive(isActive);
        validateReviewCount(reviewCount);

        this.regionId = regionId;
        this.categoryId = categoryId;
        this.userId = userId;
        this.storeName = normalizedStoreName;
        this.description = normalizedDescription;
        this.address = normalizedAddress;
        this.addressDetail = normalizedAddressDetail;
        this.phoneNumber = normalizedPhoneNumber;
        this.minOrderAmount = minOrderAmount;
        this.isOpen = isOpen;
        this.isActive = isActive;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

    /** 가게를 생성한다. */
    public static Store create(
            UUID regionId,
            UUID categoryId,
            Long userId,
            String storeName,
            String description,
            String address,
            String addressDetail,
            String phoneNumber,
            Integer minOrderAmount,
            Boolean isOpen,
            Boolean isActive,
            BigDecimal avgRating,
            Integer reviewCount
    ) {
        return Store.builder()
                .regionId(regionId)
                .categoryId(categoryId)
                .userId(userId)
                .storeName(storeName)
                .description(description)
                .address(address)
                .addressDetail(addressDetail)
                .phoneNumber(phoneNumber)
                .minOrderAmount(minOrderAmount)
                .isOpen(isOpen)
                .isActive(isActive)
                .avgRating(avgRating)
                .reviewCount(reviewCount)
                .build();
    }

    /** 가게 기본 정보를 수정한다. */
    public void update(
            UUID regionId,
            UUID categoryId,
            String storeName,
            String description,
            String address,
            String addressDetail,
            String phoneNumber,
            Integer minOrderAmount,
            Boolean isOpen,
            Boolean isActive
    ) {
        String normalizedStoreName = normalize(storeName);
        String normalizedDescription = normalize(description);
        String normalizedAddress = normalize(address);
        String normalizedAddressDetail = normalize(addressDetail);
        String normalizedPhoneNumber = normalize(phoneNumber);

        validateRegionId(regionId);
        validateCategoryId(categoryId);
        validateStoreName(normalizedStoreName);
        validateAddress(normalizedAddress);
        validateMinOrderAmount(minOrderAmount);
        validateIsOpen(isOpen);
        validateIsActive(isActive);

        this.regionId = regionId;
        this.categoryId = categoryId;
        this.storeName = normalizedStoreName;
        this.description = normalizedDescription;
        this.address = normalizedAddress;
        this.addressDetail = normalizedAddressDetail;
        this.phoneNumber = normalizedPhoneNumber;
        this.minOrderAmount = minOrderAmount;
        this.isOpen = isOpen;
        this.isActive = isActive;
    }

    /** 가게 영업 상태를 연다. */
    public void open() {
        this.isOpen = true;
    }

    /** 가게 영업 상태를 닫는다. */
    public void close() {
        this.isOpen = false;
    }

    /** 가게를 활성 상태로 변경한다. */
    public void activate() {
        this.isActive = true;
    }

    /** 가게를 비활성 상태로 변경한다. */
    public void deactivate() {
        this.isActive = false;
    }

    /** 가게의 리뷰 평점과 리뷰 수를 갱신한다. */
    public void updateRating(BigDecimal avgRating, Integer reviewCount) {
        validateReviewCount(reviewCount);

        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

    private static void validateRegionId(UUID regionId) {
        if (regionId == null) {
            throw new InvalidRegionIdException();
        }
    }

    private static void validateCategoryId(UUID categoryId) {
        if (categoryId == null) {
            throw new InvalidCategoryIdException();
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId < 1) {
            throw new InvalidUserIdException();
        }
    }

    private static void validateStoreName(String storeName) {
        if (storeName == null || storeName.isBlank()) {
            throw new InvalidStoreNameException();
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new InvalidStoreAddressException();
        }
    }

    private static void validateMinOrderAmount(Integer minOrderAmount) {
        if (minOrderAmount == null || minOrderAmount < 0) {
            throw new InvalidMinOrderAmountException();
        }
    }

    private static void validateIsOpen(Boolean isOpen) {
        if (isOpen == null) {
            throw new InvalidStoreOpenStatusException();
        }
    }

    private static void validateIsActive(Boolean isActive) {
        if (isActive == null) {
            throw new InvalidStoreActiveStatusException();
        }
    }

    private static void validateReviewCount(Integer reviewCount) {
        if (reviewCount != null && reviewCount < 0) {
            throw new InvalidReviewCountException();
        }
    }
}
