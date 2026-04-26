package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),
    INVALID_ORDER(HttpStatus.BAD_REQUEST, "ORDER-002", "주문 정보가 올바르지 않습니다."),
    INVALID_ORDER_TOTAL_PRICE(HttpStatus.BAD_REQUEST, "ORDER-003", "주문 총액은 0보다 커야 합니다."),
    INVALID_DELIVERY_ADDRESS_SNAPSHOT(HttpStatus.BAD_REQUEST, "ORDER-004", "배송지 스냅샷은 필수입니다."),
    INVALID_CANCEL_DEADLINE(HttpStatus.BAD_REQUEST, "ORDER-005", "주문 취소 가능 시간은 필수입니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER-006", "주문 상태 전이가 올바르지 않습니다."),
    CANCEL_TIME_EXCEEDED(HttpStatus.BAD_REQUEST, "ORDER-007", "주문 취소 가능 시간이 지났습니다."),
    INVALID_ORDER_ITEM_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER-008", "주문 수량은 1개 이상이어야 합니다."),
    INVALID_ORDER_ITEM_UNIT_PRICE(HttpStatus.BAD_REQUEST, "ORDER-009", "주문 단가는 0보다 커야 합니다."),
    INVALID_PRODUCT_NAME_SNAPSHOT(HttpStatus.BAD_REQUEST, "ORDER-010", "상품명 스냅샷은 필수입니다."),
    INVALID_ORDER_ITEM(HttpStatus.BAD_REQUEST, "ORDER-011", "주문 항목이 올바르지 않습니다."),
    ORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "ORDER-012", "주문에 접근할 수 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-013", "가게를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-014", "상품을 찾을 수 없습니다."),
    STORE_NOT_ORDERABLE(HttpStatus.BAD_REQUEST, "ORDER-015", "주문 가능한 가게가 아닙니다."),
    PRODUCT_NOT_ORDERABLE(HttpStatus.BAD_REQUEST, "ORDER-016", "주문 가능한 상품이 아닙니다."),
    MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "ORDER-017", "최소 주문 금액을 충족하지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
