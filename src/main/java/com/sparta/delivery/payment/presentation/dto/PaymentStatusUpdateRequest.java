package com.sparta.delivery.payment.presentation.dto;

import com.sparta.delivery.payment.domain.entity.PaymentFailureReason;
import com.sparta.delivery.payment.domain.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusUpdateRequest(
        @NotNull(message = "결제 상태는 필수입니다.")
        PaymentStatus paymentStatus,

        // FAILED 전환 시에만 사용
        PaymentFailureReason failureReason,

        // APPROVED 전환 시에만 사용 (선택값)
        String pgProvider,
        String pgTransactionId
) {
}

