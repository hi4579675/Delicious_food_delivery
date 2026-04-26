package com.sparta.delivery.order.presentation.dto;

import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderListResponse(
        UUID orderId,
        UUID storeId,
        Integer totalPrice,
        OrderStatus status,
        LocalDateTime createdAt
) {

    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
                order.getOrderId(),
                order.getStoreId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
