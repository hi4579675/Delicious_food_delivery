package com.sparta.delivery.payment.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sparta.delivery.payment.domain.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByOrderId(UUID orderId);

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByPaymentId(UUID paymentId);

    @Query(value = "select exists (select 1 from p_payment where order_id = :orderId)", nativeQuery = true)
    boolean existsAnyByOrderIdIncludingDeleted(@Param("orderId") UUID orderId);

    Page<Payment> findAllByOrderIdIn(List<UUID> orderIds, Pageable pageable);
}
