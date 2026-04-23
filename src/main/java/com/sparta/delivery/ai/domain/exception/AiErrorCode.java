package com.sparta.delivery.ai.domain.exception;


import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    INVALID_LLM_NAME(HttpStatus.BAD_REQUEST, "AI-001", "모델 이름은 100자를 넘을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
