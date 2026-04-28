package com.sparta.delivery.common.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder 빈만 단독 제공.
 *
 * SecurityConfig 에 두면 순환 참조 발생:
 *   SecurityConfig → JwtAuthenticationFilter → UserService → PasswordEncoder (= SecurityConfig)
 *
 * 분리함으로써 PasswordEncoder 를 의존하는 클래스(UserService, AuthService) 는
 * SecurityConfig 와 무관해지고 사이클이 끊어짐.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}