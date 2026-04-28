package com.sparta.delivery.order.presentation.dto;

import com.sparta.delivery.order.domain.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderItemResponse(
        UUID orderItemId,
        UUID productId,
        Integer quantity,
        Integer unitPrice,
        Integer lineTotalPrice,
        String productNameSnapshot,
        LocalDateTime createdAt
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getOrderItemId(),
                orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getLineTotalPrice(),
                orderItem.getProductNameSnapshot(),
                orderItem.getCreatedAt()
        );
    }
}
