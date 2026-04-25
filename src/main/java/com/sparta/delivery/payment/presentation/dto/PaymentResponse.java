package com.sparta.delivery.payment.presentation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sparta.delivery.payment.domain.entity.Payment;
import com.sparta.delivery.payment.domain.entity.PaymentFailureReason;
import com.sparta.delivery.payment.domain.entity.PaymentMethod;
import com.sparta.delivery.payment.domain.entity.PaymentStatus;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        Integer totalPrice,
        LocalDateTime approvedAt,
        LocalDateTime failedAt,
        LocalDateTime cancelledAt,
        PaymentFailureReason failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getTotalPrice(),
                payment.getApprovedAt(),
                payment.getFailedAt(),
                payment.getCancelledAt(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
