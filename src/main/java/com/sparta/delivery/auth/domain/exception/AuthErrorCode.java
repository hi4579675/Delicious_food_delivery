package com.sparta.delivery.auth.domain.exception;

import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-002", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "유효하지 않은 토큰입니다."),
    TOKEN_VERSION_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH-004", "토큰이 더 이상 유효하지 않습니다. 재로그인이 필요합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
