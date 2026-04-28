package com.sparta.delivery.auth.application;

import com.sparta.delivery.auth.domain.exception.InvalidCredentialsException;
import com.sparta.delivery.auth.infrastructure.jwt.JwtProvider;
import com.sparta.delivery.auth.presentation.dto.LoginResponse;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.exception.UserNotFoundException;
import com.sparta.delivery.user.presentation.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LoginResponse login(LoginRequest request) {
        // 1. 이메일로 유저 조회. 없으면 InvalidCredentials로 통일 (존재 여부 노출 방지)
        User user;
        try {
            user = userService.findByEmail(request.email());
        } catch (UserNotFoundException e) {
            log.debug("로그인 실패 (이메일 없음)");
            throw new InvalidCredentialsException();
        }

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.debug("로그인 실패 (비밀번호 불일치): userId={}", user.getUserId());
            throw new InvalidCredentialsException();
        }

        // 3. 토큰 발급 (userId, role, 현재 tokenVersion 포함)
        String token = jwtProvider.generateToken(
                user.getUserId(),
                user.getRole().name(),
                user.getTokenVersion()
        );

        // 4. 마지막 로그인 시각 갱신
        userService.updateLastLoginAt(user.getUserId());

        log.info("로그인 성공: userId={}", user.getUserId());

        return new LoginResponse(
                token,
                user.getUserId(),
                user.getRole().name(),
                jwtProvider.getExpirationMs()
        );
    }

    /**
     * 로그아웃.
     *
     * 현재 tokenVersion 단위 무효화 구조라 "이 디바이스만 로그아웃" 은 불가능 — 호출 시 해당 유저의
     * 모든 기존 JWT 가 즉시 무효화된다. 디바이스별 분리는 추후 RefreshToken 도입 시 재설계.
     *
     * 실제 무효화는 UserService.forceLogout 에 위임 (도메인 흐름 유지).
     */
    public void logout(Long userId) {
        userService.forceLogout(userId);
        log.info("로그아웃: userId={}", userId);
    }

}
