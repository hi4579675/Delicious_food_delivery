package com.sparta.delivery.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    /** HTTP 응답 상태 */
    HttpStatus getStatus();

    /** 클라이언트 식별용 코드 (예: "USER-001") */
    String getCode();

    /** 사용자 메시지 */
    String getMessage();
}