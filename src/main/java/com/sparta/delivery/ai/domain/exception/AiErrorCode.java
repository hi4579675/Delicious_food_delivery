package com.sparta.delivery.ai.domain.exception;


import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    INVALID_LLM_NAME(HttpStatus.BAD_REQUEST, "AI-001", "모델 이름은 100자를 넘을 수 없습니다."),
    INVALID_LLM_PROVIDER(HttpStatus.BAD_REQUEST, "AI-002", "유효하지 않은 LLM provider입니다."),
    INVALID_INPUT_SNAPSHOT(HttpStatus.BAD_REQUEST, "AI-003", "inputSnapshot은 비어 있을 수 없습니다."),
    INVALID_PROVIDER_STATUS_CODE(HttpStatus.BAD_REQUEST, "AI-004", "providerStatusCode는 50자를 넘을 수 없습니다."),
    INVALID_CREATED_BY(HttpStatus.BAD_REQUEST, "AI-005", "createdBy는 필수입니다."),

    AI_FORBIDDEN(HttpStatus.FORBIDDEN, "AI-101", "모델 정보에 접근할 수 없습니다."),

    DUPLICATE_LLM_NAME(HttpStatus.CONFLICT, "AI-201", "이미 존재하는 모델입니다."),
    LLM_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-202", "모델을 찾을 수 없습니다."),
    CANNOT_DELETE_ACTIVE_LLM(HttpStatus.BAD_REQUEST, "AI-203", "활성 모델은 삭제할 수 없습니다."),

    LLM_CALL_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-301", "LLM 호출 로그를 찾을 수 없습니다."),
    EXTERNAL_LLM_CALL_FAILED(HttpStatus.BAD_GATEWAY, "AI-302", "외부 LLM 호출에 실패했습니다."),
    ACTIVE_LLM_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-303", "활성화된 LLM을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
