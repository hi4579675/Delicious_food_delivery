package com.sparta.delivery.auth.application;

import com.sparta.delivery.auth.domain.exception.InvalidCredentialsException;
import com.sparta.delivery.auth.infrastructure.jwt.JwtProvider;
import com.sparta.delivery.auth.presentation.dto.LoginResponse;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.entity.UserRole;
import com.sparta.delivery.user.domain.exception.UserNotFoundException;
import com.sparta.delivery.user.presentation.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * AuthService 단위 테스트.
 *
 * 검증 포인트:
 *  - 정상 로그인 시 토큰 발급 + lastLoginAt 갱신 호출
 *  - 이메일 없음 / 비번 불일치 모두 동일한 InvalidCredentialsException (열거 공격 방어)
 *  - UserService / JwtProvider / PasswordEncoder 협력 검증
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserService userService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtProvider jwtProvider;

    @InjectMocks
    AuthService authService;

    private User createUser(Long userId, String email, UserRole role, String encoded, int tokenVersion) {
        User user = User.create(email, encoded, "alice01", "01012345678", role);
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "tokenVersion", tokenVersion);
        return user;
    }

    @Test
    @DisplayName("정상 로그인 - 토큰 발급 + lastLoginAt 갱신")
    void login_success() {
        User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "ENCODED", 0);
        LoginRequest req = new LoginRequest("alice@test.com", "rawPw");

        given(userService.findByEmail("alice@test.com")).willReturn(user);
        given(passwordEncoder.matches("rawPw", "ENCODED")).willReturn(true);
        given(jwtProvider.generateToken(1L, "CUSTOMER", 0)).willReturn("JWT_TOKEN");
        given(jwtProvider.getExpirationMs()).willReturn(3600000L);

        LoginResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("JWT_TOKEN");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo("CUSTOMER");
        assertThat(response.expiresIn()).isEqualTo(3600000L);
        verify(userService).updateLastLoginAt(1L);
    }

    @Test
    @DisplayName("이메일 없음 - InvalidCredentialsException (존재 여부 노출 방지)")
    void login_email_not_found() {
        LoginRequest req = new LoginRequest("no@test.com", "rawPw");
        given(userService.findByEmail("no@test.com")).willThrow(new UserNotFoundException());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtProvider, never()).generateToken(anyLong(), anyString(), anyInt());
        verify(userService, never()).updateLastLoginAt(anyLong());
    }

    @Test
    @DisplayName("비밀번호 불일치 - InvalidCredentialsException")
    void login_wrong_password() {
        User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "ENCODED", 0);
        LoginRequest req = new LoginRequest("alice@test.com", "wrong");

        given(userService.findByEmail("alice@test.com")).willReturn(user);
        given(passwordEncoder.matches("wrong", "ENCODED")).willReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtProvider, never()).generateToken(anyLong(), anyString(), anyInt());
        verify(userService, never()).updateLastLoginAt(anyLong());
    }

    @Test
    @DisplayName("토큰 발급 시 현재 tokenVersion 이 반영됨")
    void login_token_uses_current_version() {
        User user = createUser(1L, "alice@test.com", UserRole.MANAGER, "ENCODED", 5);
        LoginRequest req = new LoginRequest("alice@test.com", "rawPw");

        given(userService.findByEmail("alice@test.com")).willReturn(user);
        given(passwordEncoder.matches("rawPw", "ENCODED")).willReturn(true);
        given(jwtProvider.generateToken(1L, "MANAGER", 5)).willReturn("JWT");
        given(jwtProvider.getExpirationMs()).willReturn(3600000L);

        authService.login(req);

        verify(jwtProvider).generateToken(1L, "MANAGER", 5);
    }

    @Test
    @DisplayName("로그아웃 - userService.forceLogout 위임 호출")
    void logout_delegates_to_userService() {
        // when
        authService.logout(1L);

        // then : 위임만 검증. tokenVersion 증가 검증은 UserServiceTest 에서 담당.
        verify(userService).forceLogout(1L);
    }

    @Test
    @DisplayName("로그인 호출 순서 - 비번 검증 → 토큰 발급 → lastLoginAt 갱신")
    void login_call_order() {
        // given
        User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "ENCODED", 0);
        LoginRequest req = new LoginRequest("alice@test.com", "rawPw");
        given(userService.findByEmail("alice@test.com")).willReturn(user);
        given(passwordEncoder.matches("rawPw", "ENCODED")).willReturn(true);
        given(jwtProvider.generateToken(1L, "CUSTOMER", 0)).willReturn("JWT");
        given(jwtProvider.getExpirationMs()).willReturn(3600000L);

        // when
        authService.login(req);

        // then : 비번 통과 전에 토큰이 발급되거나 lastLoginAt 이 갱신되는 회귀를 잡는다.
        InOrder inOrder = inOrder(passwordEncoder, jwtProvider, userService);
        inOrder.verify(passwordEncoder).matches("rawPw", "ENCODED");
        inOrder.verify(jwtProvider).generateToken(1L, "CUSTOMER", 0);
        inOrder.verify(userService).updateLastLoginAt(1L);
    }

    @Test
    @DisplayName("로그아웃 - userService.forceLogout 정확히 1회 호출 + 다른 협력자 무호출")
    void logout_called_once_only() {
        // when
        authService.logout(1L);

        // then : 로그아웃은 토큰/비번 관련 협력자를 건드리지 않아야 한다.
        verify(userService, times(1)).forceLogout(1L);
        verifyNoInteractions(passwordEncoder, jwtProvider);
    }

    @Test
    @DisplayName("로그아웃 - forceLogout 에서 예외 발생 시 그대로 전파 (catch 금지)")
    void logout_propagates_exception() {
        // given : 존재하지 않는 유저 → UserService 가 던지는 예외
        willThrow(new UserNotFoundException()).given(userService).forceLogout(999L);

        // when / then : AuthService 가 임의로 변환/삼키지 않음
        assertThatThrownBy(() -> authService.logout(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

}