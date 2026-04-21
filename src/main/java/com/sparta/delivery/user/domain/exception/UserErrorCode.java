package com.sparta.delivery.user.domain.exception;

import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER-003", "비밀번호가 올바르지 않습니다."),
    FORBIDDEN_ROLE_CHANGE(HttpStatus.FORBIDDEN, "USER-004", "권한을 변경할 수 없습니다."),
    ALREADY_DELETED(HttpStatus.BAD_REQUEST, "USER-005", "이미 탈퇴한 사용자입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "USER-006", "유효하지 않은 역할입니다. (CUSTOMER, OWNER, MANAGER, MASTER)");

    private final HttpStatus status;
    private final String code;
    private final String message;
}