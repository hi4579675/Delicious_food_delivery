package com.sparta.delivery.order.domain.repository;

import com.sparta.delivery.order.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByStoreIdOrderByCreatedAtDesc(UUID storeId);
}
