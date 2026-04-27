package com.sparta.delivery.auth.infrastructure.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.auth.domain.exception.InvalidTokenException;
import com.sparta.delivery.auth.domain.exception.TokenExpiredException;
import com.sparta.delivery.auth.domain.exception.TokenVersionMismatchException;
import com.sparta.delivery.auth.infrastructure.security.UserPrincipalImpl;
import com.sparta.delivery.common.exception.BaseException;
import com.sparta.delivery.common.exception.ErrorCode;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.exception.UserNotFoundException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 1. 헤더에서 토큰 추출
        String token = extractToken(request);

        // 2. 토큰 없거나 이미 인증된 경우 → 바로 체인 통과 (공개 엔드포인트이거나 다른 필터가 이미 인증 처리)
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        // 3. 인증 세팅 시도. 실패 시 즉시 401 응답 + return (체인 진행 X)
        //    try 범위는 "인증 관련 로직" 으로 한정 — chain.doFilter 는 밖으로.
        try {
            authenticate(request, token);
        } catch (TokenExpiredException | InvalidTokenException | TokenVersionMismatchException e) {
            // 인증 실패 — 401 응답
            SecurityContextHolder.clearContext();
            log.debug("JWT 인증 실패: {} {} → {}",
                    request.getMethod(), request.getRequestURI(), e.getErrorCode().getCode());
            writeError(response, e.getErrorCode());
            return;   // ← 여기서 끝. chain.doFilter 호출하지 않음
        }

        // 4. 인증 성공 → 체인 진행.
        chain.doFilter(request, response);
    }

    /**
     * 토큰 파싱 → DB 조회 → tokenVersion 비교 → SecurityContext 세팅.
     * 실패 시 위 3개 예외 중 하나를 던짐 (호출자가 catch).
     */
    private void authenticate(HttpServletRequest request, String token) {
        // 3-1. 토큰 파싱 (만료/위조/claim누락 시 예외)
        JwtProvider.TokenPayload payload = jwtProvider.parse(token);

        // 3-2. DB 조회 (탈퇴 유저는 @SQLRestriction 때문에 UserNotFoundException -> 토큰 무효로 재매핑)
        User user;
        try {
            user = userService.findById(payload.userId());
        } catch (UserNotFoundException e) {
            throw new TokenVersionMismatchException();
        }

        // 3-3. tokenVersion 일치 확인 (역할 변경/ 비번 변경/ 탈퇴 시 DB 값이 증가해있음)
        if (user.getTokenVersion() != payload.tokenVersion()) {
            throw new TokenVersionMismatchException();
        }

        // 3-4. DB의 최신 User 로 UserPrincipal 생성 (JWT claim role 이 아닌 DB role 사용)
        UserPrincipalImpl principal = UserPrincipalImpl.from(user);

        // 3-5. Authentication 객체 생성 (credentails=null - 토큰으로 이미 검증됨)
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // 3-6. 요청 Ip/sessionId 등 부가 정보를 details에 기록
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 3-7. SecurityContext setting -> 이후 @AuthenticationPrincipal로 주입 가능
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    /**
     * "Authorization: Bearer {token}" 헤더에서 토큰 부분만 추출.
     * 헤더 없음 / Bearer prefix 없음 / prefix 뒤 공백뿐인 경우 모두 null.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) return null;
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * 인증 실패 응답 작성.
     * GlobalExceptionHandler 와 동일한 ApiResponse 포맷 유지.
     */
    private void writeError(HttpServletResponse response, ErrorCode ec) throws IOException {
        response.setStatus(ec.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(ec));
    }

    /**
     * 필터 스킵 대상 — "토큰 없이 호출되는 게 정상" 인 엔드포인트만 화이트리스트.
     *
     * 주의: /api/v1/auth/** 전체를 스킵하면 안 됨. /auth/logout 은 인증된 요청이어야 하므로
     * 필터를 타고 SecurityContext 에 principal 이 채워져야 한다. 따라서 /auth 하위는
     * 무인증 경로(login 등)만 명시적으로 enumerate.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean isAuthLogin = "/api/v1/auth/login".equals(path)
                && "POST".equalsIgnoreCase(request.getMethod());
        boolean isSignup = "/api/v1/users/signup".equals(path)
                && "POST".equalsIgnoreCase(request.getMethod());
        return isAuthLogin
                || isSignup
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info");
    }

}
