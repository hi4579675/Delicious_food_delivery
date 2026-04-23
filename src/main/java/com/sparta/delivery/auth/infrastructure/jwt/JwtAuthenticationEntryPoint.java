package com.sparta.delivery.auth.infrastructure.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.common.exception.CommonErrorCode;
import com.sparta.delivery.common.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청이 보호된 엔드포인트에 접근했을 때 호출되는 폴백.
 *
 * 언제 호출되는가:
 *  - Authorization 헤더 없이 /api/v1/users/me/** 등 보호 엔드포인트 접근
 *  - JwtAuthenticationFilter 가 응답을 이미 쓴 경우(토큰 파싱 실패 등)는 여기까지 안 옴
 *
 * 책임:
 *  - Spring Security 기본 401 응답 (WWW-Authenticate 헤더, 빈 본문) 을 우리 ApiResponse 포맷으로 통일
 *
 * 왜 필요한가:
 *  - 필터에서 token==null 시 체인 통과하면 다운스트림 인가 단계에서 실패
 *  - 기본 AuthenticationEntryPoint 는 프로젝트 응답 포맷과 다른 JSON/HTML 반환
 *  - 일관된 응답 포맷 유지를 위해 커스텀 EntryPoint 등록 필요
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // 필터에서 이미 응답을 쓴 경우 중복 작성 방지
        if (response.isCommitted()) {
            return;
        }
        log.debug("인증되지 않은 요청: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(CommonErrorCode.UNAUTHORIZED.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(CommonErrorCode.UNAUTHORIZED));


    }
}
