package com.sparta.delivery.common.config.jpa;

import com.sparta.delivery.common.config.security.UserPrincipal;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // SecurityContext 자체가 비어있는 경우 (필터 체인 밖, 일부 비동기/스케줄러 컨텍스트 등).
            // 익명 사용자는 여기 걸리지 않고 — AnonymousAuthenticationToken 은 isAuthenticated()=true 반환 —
            // 아래 principal 문자열 체크에서 걸러진다.
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.empty();
            }

            // 회원가입/Swagger/헬스체크처럼 permitAll() 인 요청은 익명 토큰으로 들어오며
            // 이때 principal 은 "anonymousUser" 문자열이다. createdBy 기록 대상 아님.
            Object principal = auth.getPrincipal();
            if (principal instanceof String) {
                return Optional.empty();
            }

            // 인증된 사용자 — UserPrincipal 구현체에서 user_id 추출
            if (principal instanceof UserPrincipal userPrincipal) {
                return Optional.of(userPrincipal.getId());
            }
            log.warn("Unknown principal type: {}", auth.getPrincipal().getClass());
            return Optional.empty();
        };
    }
}
