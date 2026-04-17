package com.sparta.delivery.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sparta.delivery.common.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String errorCode,
        String message,
        T data
) {

    // ========== 성공 ==========

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, null, "OK", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 200, null, message, data);
    }

    /** 데이터 없는 200 응답 (record 접근자와 충돌 피하려고 ok() 로 명명) */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, 200, null, "OK", null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, 201, null, "Created", data);
    }

    // ========== 실패 ==========

    /** ErrorCode 인터페이스 기반 — 어느 도메인 enum이든 받을 수 있음 */
    public static <T> ApiResponse<T> error(ErrorCode ec) {
        return new ApiResponse<>(false, ec.getStatus().value(), ec.getCode(), ec.getMessage(), null);
    }

    /** ErrorCode + 추가 데이터 (Validation 필드 에러 등) */
    public static <T> ApiResponse<T> error(ErrorCode ec, T data) {
        return new ApiResponse<>(false, ec.getStatus().value(), ec.getCode(), ec.getMessage(), data);
    }

    /** 직접 구성 (fallback용) */
    public static <T> ApiResponse<T> error(int status, String errorCode, String message) {
        return new ApiResponse<>(false, status, errorCode, message, null);
    }
}