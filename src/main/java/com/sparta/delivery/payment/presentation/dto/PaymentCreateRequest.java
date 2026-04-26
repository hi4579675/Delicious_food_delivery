package com.sparta.delivery.payment.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.sparta.delivery.payment.domain.entity.PaymentMethod;

public record PaymentCreateRequest (
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "결제 수단은 필수입니다.")
        PaymentMethod paymentMethod,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Integer totalPrice
) {
}