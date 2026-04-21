package com.sparta.delivery.payment.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sparta.delivery.payment.domain.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<Payment> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<Payment> findByPaymentIdAndDeletedAtIsNull(UUID paymentId);

}
