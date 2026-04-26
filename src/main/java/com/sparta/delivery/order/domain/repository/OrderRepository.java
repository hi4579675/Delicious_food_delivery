package com.sparta.delivery.order.domain.repository;

import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findAllByUserId(Long userId, Pageable pageable);

    Page<Order> findAllByUserIdAndStoreId(Long userId, UUID storeId, Pageable pageable);

    Page<Order> findAllByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    Page<Order> findAllByUserIdAndStoreIdAndStatus(
            Long userId,
            UUID storeId,
            OrderStatus status,
            Pageable pageable
    );

    Page<Order> findAllByStoreId(UUID storeId, Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findAllByStoreIdAndStatus(UUID storeId, OrderStatus status, Pageable pageable);

    Page<Order> findAllByStoreIdIn(List<UUID> storeIds, Pageable pageable);

    Page<Order> findAllByStoreIdInAndStatus(List<UUID> storeIds, OrderStatus status, Pageable pageable);
}
