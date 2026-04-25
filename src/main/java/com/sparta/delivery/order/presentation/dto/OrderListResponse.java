package com.sparta.delivery.order.presentation.dto;

import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderListResponse(
        UUID orderId,
        UUID storeId,
        UUID addressId,
        Long userId,
        Integer totalPrice,
        OrderStatus status,
        String deliveryAddressSnapshot,
        LocalDateTime cancelDeadlineAt,
        LocalDateTime rejectedAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
                order.getOrderId(),
                order.getStoreId(),
                order.getAddressId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getDeliveryAddressSnapshot(),
                order.getCancelDeadlineAt(),
                order.getRejectedAt(),
                order.getCompletedAt(),
                order.getCanceledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
