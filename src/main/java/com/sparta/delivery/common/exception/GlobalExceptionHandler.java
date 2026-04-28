package com.sparta.delivery.common.exception;

import com.sparta.delivery.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 모든 도메인 예외 — BaseException 다형성 */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBase(BaseException e, HttpServletRequest req) {
        ErrorCode ec = e.getErrorCode();
        log.warn("[{}] {} - {}", ec.getCode(), req.getRequestURI(), e.getMessage());
        return ResponseEntity.status(ec.getStatus()).body(ApiResponse.error(ec));
    }

    /** @Valid 검증 실패 — 필드 에러 상세를 data에 담는다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest req) {
        List<FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(FieldErrorDetail::from)
                .toList();
        log.warn("[Validation] {} - {} fields failed", req.getRequestURI(), details.size());
        return ResponseEntity
                .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, details));
    }

    /** HTTP 메서드 미지원 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    /** Spring Security 인증 실패 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(
            AuthenticationException e, HttpServletRequest req) {
        log.warn("[Auth] {} - {}", req.getRequestURI(), e.getMessage());
        return ResponseEntity.status(CommonErrorCode.UNAUTHORIZED.getStatus())
                .body(ApiResponse.error(CommonErrorCode.UNAUTHORIZED));
    }

    /** Spring Security 권한 부족 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException e, HttpServletRequest req) {
        log.warn("[Forbidden] {} - {}", req.getRequestURI(), e.getMessage());
        return ResponseEntity.status(CommonErrorCode.FORBIDDEN.getStatus())
                .body(ApiResponse.error(CommonErrorCode.FORBIDDEN));
    }

    /** 최종 fallback — 스택트레이스는 로그에만 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleFallback(Exception e, HttpServletRequest req) {
        log.error("[Unhandled] {} - {}", req.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

    /** JSON 파싱 실패 / enum 값 범위 밖 등 — 잘못된 요청 본문 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest req) {
        Throwable cause = e.getMostSpecificCause();
        String causeType = (cause == null ? e : cause).getClass().getSimpleName();
        log.warn("[InvalidBody] {} - {}", req.getRequestURI(), causeType);
        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    /** Validation 필드 에러 상세 (응답 data에 담김) */
    public record FieldErrorDetail(String field, String value, String reason) {
        public static FieldErrorDetail from(FieldError fe) {
            return new FieldErrorDetail(
                    fe.getField(),
                    fe.getRejectedValue() == null ? "" : fe.getRejectedValue().toString(),
                    fe.getDefaultMessage()
            );
        }
    }
}