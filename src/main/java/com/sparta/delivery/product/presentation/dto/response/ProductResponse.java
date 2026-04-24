package com.sparta.delivery.product.presentation.dto.response;

import com.sparta.delivery.product.domain.entity.DescriptionSource;
import com.sparta.delivery.product.domain.entity.Product;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        UUID storeId,
        String productName,
        String description,
        DescriptionSource descriptionSource,
        Integer price,
        boolean isSoldOut,
        boolean isHidden,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product){
        return new ProductResponse(
                product.getProductId(),
                product.getStoreId(),
                product.getProductName(),
                product.getDescription(),
                product.getDescriptionSource(),
                product.getPrice(),
                product.isSoldOut(),
                product.isHidden(),
                product.getDisplayOrder(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
