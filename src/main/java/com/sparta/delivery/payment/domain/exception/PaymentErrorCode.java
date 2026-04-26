package com.sparta.delivery.payment.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.sparta.delivery.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    INVALID_ORDER_ID(HttpStatus.BAD_REQUEST, "PAYMENT-001", "유효하지 않은 주문 ID입니다."),
    INVALID_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "PAYMENT-002", "결제 방법은 CARD만 선택 가능합니다."),
    INVALID_TOTAL_PRICE(HttpStatus.BAD_REQUEST, "PAYMENT-003", "전체 가격은 0보다 커야 합니다."),
    INVALID_ORDER_STATUS_FOR_PAYMENT(HttpStatus.BAD_REQUEST, "PAYMENT-009", "결제 가능한 주문 상태가 아닙니다."),
    INVALID_PAYMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "PAYMENT-004", "결제 상태 전이가 올바르지 않습니다."),
    INVALID_PAYMENT_FAILURE_REASON(HttpStatus.BAD_REQUEST, "PAYMENT-008", "결제 실패 사유는 필수입니다."),

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT-005", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "PAYMENT-006", "결제에 접근할 수 없습니다."),
    DUPLICATE_PAYMENT_ORDER(HttpStatus.CONFLICT, "PAYMENT-007", "이미 해당 주문에 대한 결제가 존재합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
