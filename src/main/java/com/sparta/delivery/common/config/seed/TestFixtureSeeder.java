package com.sparta.delivery.common.config.seed;

import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.entity.UserRole;
import com.sparta.delivery.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 로컬 테스트용 공통 계정 시드.
 * local 프로필에서만 실행. 이미 있으면 스킵 (멱등).
 *
 * 모든 계정의 비밀번호: Pass1234!
 */
@Slf4j
@Profile("local")
@Component
@Order(1)
@RequiredArgsConstructor
public class TestFixtureSeeder implements CommandLineRunner {

    private static final String COMMON_PASSWORD = "Pass1234!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("master@test.com",  "마스터",  "01000000000", UserRole.MASTER);
        seed("manager@test.com", "매니저1", "01000000001", UserRole.MANAGER);
        seed("owner1@test.com",  "사장1",   "01011111111", UserRole.OWNER);
        seed("owner2@test.com",  "사장2",   "01022222222", UserRole.OWNER);
        seed("alice@test.com",   "앨리스",  "01033333333", UserRole.CUSTOMER);
        seed("bob@test.com",     "밥",      "01044444444", UserRole.CUSTOMER);
        seed("charlie@test.com", "찰리",    "01055555555", UserRole.CUSTOMER);

        log.info("""
                
                [Seed] 테스트 계정 준비 완료 (비밀번호 전부 '{}')
                ├ MASTER   : master@test.com
                ├ MANAGER  : manager@test.com
                ├ OWNER    : owner1@test.com, owner2@test.com
                └ CUSTOMER : alice@test.com, bob@test.com, charlie@test.com
                """, COMMON_PASSWORD);
    }

    private void seed(String email, String name, String phone, UserRole role) {
        if (userRepository.existsByEmail(email)) return;
        userRepository.save(User.create(
                email,
                passwordEncoder.encode(COMMON_PASSWORD),
                name, phone, role
        ));
    }
}