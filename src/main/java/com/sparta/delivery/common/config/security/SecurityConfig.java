package com.sparta.delivery.common.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationEntryPoint;
import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize("hasRole('MASTER')") 등 메서드 레벨 권한 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- 공개 엔드포인트 ---
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // /auth 하위는 무인증 경로만 명시. /auth/logout 등 인증 필요한 경로는
                        // anyRequest().authenticated() 에 흡수시킨다.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/signup")
                        .permitAll()
                        // 관리자 전용 조회 엔드포인트
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/regions/inactive",
                                "/api/v1/store-categories/inactive",
                                "/api/v1/store-categories/all",
                                "/api/v1/stores/inactive"
                        ).hasAnyRole("MANAGER", "MASTER")
                        // 공개 조회 엔드포인트
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/*",
                                "/api/v1/regions",
                                "/api/v1/regions/root",
                                "/api/v1/regions/*",
                                "/api/v1/regions/*/children",
                                "/api/v1/store-categories",
                                "/api/v1/store-categories/*",
                                "/api/v1/stores",
                                "/api/v1/stores/*",
                                "/api/v1/stores/*/products"
                        ).permitAll()
                        // --- 그 외 전부 인증 필요 ---
                        .anyRequest().authenticated()
                )
                // 추가 : 토큰 없이 보호 엔드포인트 접근 시 우리 포맷으로 401
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();

    }

}
