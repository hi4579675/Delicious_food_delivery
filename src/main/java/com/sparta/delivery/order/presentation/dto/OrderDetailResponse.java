package com.sparta.delivery.order.presentation.dto;

import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
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
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
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
                order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
