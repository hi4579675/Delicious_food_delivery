package com.sparta.delivery.ai.domain.exception;


import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    INVALID_LLM_NAME(HttpStatus.BAD_REQUEST, "AI-001", "모델 이름은 100자를 넘을 수 없습니다."),

    LLM_FORBIDDEN(HttpStatus.FORBIDDEN, "AI-101", "모델 정보에 접근할 수 없습니다."),
    DUPLICATE_LLM_NAME(HttpStatus.CONFLICT, "AI-102", "이미 존재하는 모델입니다."),
    LLM_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-103", "모델을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
