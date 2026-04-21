package com.sparta.delivery.ai.domain.exception;


import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    INVALID_LLM_NAME(HttpStatus.BAD_REQUEST, "AI-001", "유효하지 않은 모델 이름입니다."),
    INVALID_INPUT_SNAPSHOT(HttpStatus.BAD_REQUEST, "AI-002", "유효하지 않은 입력 스냅샷입니다."),
    INVALID_CREATED_BY(HttpStatus.BAD_REQUEST, "AI-003", "유효하지 않은 생성자 정보입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
