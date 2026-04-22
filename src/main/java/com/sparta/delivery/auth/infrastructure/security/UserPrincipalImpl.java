package com.sparta.delivery.auth.infrastructure.security;

import com.sparta.delivery.common.config.security.UserPrincipal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.sparta.delivery.user.domain.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPrincipalImpl implements UserPrincipal, UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final int tokenVersion;

    /** User 엔티티로부터 Principal 생성 — 단일 진입점 */
    public static UserPrincipalImpl from(User user) {
        return new UserPrincipalImpl(
                user.getUserId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().name(),
                user.getTokenVersion()
        );
    }

    // ===== UserPrincipal (팀 공통 인터페이스) =====

    @Override
    public Long getId() {
        return id;
    }

    /** 팀 공통 인터페이스의 getUsername — 로그인 식별자 = 이메일 */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getRole() {
        return role;
    }

    // ===== UserDetails (Spring Security) =====

    /**
     * @PreAuthorize 가 매칭에 사용할 authority 목록.
     * "ROLE_" prefix 필수 — 이게 없으면 hasRole('MASTER') 가 매치되지 않음.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

}
