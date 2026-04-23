package com.sparta.delivery.address.domain.exception;

import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AddressErrorCode implements ErrorCode {

    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDRESS-001", "배송지를 찾을 수 없습니다."),
    ADDRESS_FORBIDDEN(HttpStatus.FORBIDDEN, "ADDRESS-002", "본인 배송지만 접근할 수 있습니다."),
    INVALID_ADDRESS(HttpStatus.BAD_REQUEST, "ADDRESS-003", "주소는 필수 입력값입니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "ADDRESS-004", "사용자 ID는 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
