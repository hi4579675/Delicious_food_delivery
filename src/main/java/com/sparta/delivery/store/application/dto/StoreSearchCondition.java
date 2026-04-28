package com.sparta.delivery.store.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StoreSearchCondition(
        UUID regionId,
        UUID categoryId,
        Long userId,
        Boolean isOpen,
        Boolean isActive,
        String keyword,
        String addressKeyword,
        BigDecimal minRating,
        Integer minReviewCount,
        Integer maxMinOrderAmount,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
) {
}
