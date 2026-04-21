package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    INVALID_STORE_ID(HttpStatus.BAD_REQUEST, "PRODUCT-001", "유효하지 않은 가게 ID입니다."),
    INVALID_PRODUCT_NAME(HttpStatus.BAD_REQUEST, "PRODUCT-002", "상품명은 비어 있을 수 없습니다."),
    INVALID_PRODUCT_DESCRIPTION(HttpStatus.BAD_REQUEST, "PRODUCT-003", "상품 설명과 설명 출처의 조합이 올바르지 않습니다."),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "PRODUCT-004", "상품 가격은 0보다 커야 합니다."),
    INVALID_DISPLAY_ORDER(HttpStatus.BAD_REQUEST, "PRODUCT-005", "노출 순서는 0 이상이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
