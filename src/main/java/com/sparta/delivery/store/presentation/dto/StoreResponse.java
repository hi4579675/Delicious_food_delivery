package com.sparta.delivery.store.presentation.dto;

import com.sparta.delivery.store.domain.entity.Store;
import java.math.BigDecimal;
import java.util.UUID;

public record StoreResponse(
        UUID storeId,
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

    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getStoreId(),
                store.getRegionId(),
                store.getCategoryId(),
                store.getUserId(),
                store.getStoreName(),
                store.getDescription(),
                store.getAddress(),
                store.getAddressDetail(),
                store.getPhoneNumber(),
                store.getMinOrderAmount(),
                store.getIsOpen(),
                store.getIsActive(),
                store.getAvgRating(),
                store.getReviewCount()
        );
    }
}
