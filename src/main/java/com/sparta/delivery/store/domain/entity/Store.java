package com.sparta.delivery.store.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "p_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
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
        this.regionId = regionId;
        this.categoryId = categoryId;
        this.userId = userId;
        this.storeName = storeName;
        this.description = description;
        this.address = address;
        this.addressDetail = addressDetail;
        this.phoneNumber = phoneNumber;
        this.minOrderAmount = minOrderAmount;
        this.isOpen = isOpen;
        this.isActive = isActive;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

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
        return new Store(
                regionId,
                categoryId,
                userId,
                storeName,
                description,
                address,
                addressDetail,
                phoneNumber,
                minOrderAmount,
                isOpen,
                isActive,
                avgRating,
                reviewCount
        );
    }

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
        this.regionId = regionId;
        this.categoryId = categoryId;
        this.storeName = storeName;
        this.description = description;
        this.address = address;
        this.addressDetail = addressDetail;
        this.phoneNumber = phoneNumber;
        this.minOrderAmount = minOrderAmount;
        this.isOpen = isOpen;
        this.isActive = isActive;
    }

    public void open() {
        this.isOpen = true;
    }

    public void close() {
        this.isOpen = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateRating(BigDecimal avgRating, Integer reviewCount) {
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }
}
