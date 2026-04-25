package com.sparta.delivery.payment.domain.entity;

/**
 * 결제 실패 사유 코드.
 * - 외부(PG) 실패/거절 사유를 "문자열"로 저장하지 않고, 코드(enum)로 표준화한다.
 */
public enum PaymentFailureReason {
    PG_TIMEOUT,
    INSUFFICIENT_FUNDS,
    UNKNOWN
}

