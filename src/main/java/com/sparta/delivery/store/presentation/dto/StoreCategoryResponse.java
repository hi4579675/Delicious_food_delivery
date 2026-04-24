package com.sparta.delivery.store.presentation.dto;

import com.sparta.delivery.store.domain.entity.StoreCategory;
import java.util.UUID;

public record StoreCategoryResponse(
        UUID categoryId,
        String categoryName,
        String description,
        Integer sortOrder,
        Boolean isActive
) {

    public static StoreCategoryResponse from(StoreCategory storeCategory) {
        return new StoreCategoryResponse(
                storeCategory.getCategoryId(),
                storeCategory.getCategoryName(),
                storeCategory.getDescription(),
                storeCategory.getSortOrder(),
                storeCategory.getIsActive()
        );
    }
}
