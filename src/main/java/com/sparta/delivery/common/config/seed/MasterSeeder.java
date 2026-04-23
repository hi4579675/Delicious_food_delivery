package com.sparta.delivery.common.config.seed;


import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.entity.UserRole;
import com.sparta.delivery.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static reactor.netty.http.HttpConnectionLiveness.log;

/**
 * 로컬 개발용 MASTER 계정 시드.
 *
 * 왜 필요한가:
 *  - 회원가입 API 는 MASTER/MANAGER 자가등록을 원천 차단 (SignupRole enum 으로).
 *  - MASTER 없이는 역할 변경 / 관리자 기능 테스트 불가능.
 *  - 따라서 최초 MASTER 는 시드로 부트스트랩.
 *
 * 운영 방침:
 *  - @Profile("local") 로 로컬 환경에서만 실행. prod 에선 절대 동작 X.
 *  - 이미 같은 이메일이 존재하면 재생성 X (멱등성).
 *  - MASTER 생성 후엔 이 계정으로 다른 유저의 role 을 변경해서 MANAGER 등 파생 가능.
 *
 * 테스트 계정:
 *  - email:    master@test.com
 *  - password: Pass1234!
 *  - name:     master01
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class MasterSeeder implements CommandLineRunner {

    private static final String MASTER_EMAIL = "master@test.com";
    private static final String MASTER_RAW_PASSWORD = "Pass1234!";
    private static final String MASTER_NAME = "master01";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 이미 있으면 스킵 — 앱 재시작 시마다 중복 생성 방지
        if (userRepository.existsByEmail(MASTER_EMAIL)) {
            log.info("[Seed] MASTER 계정 이미 존재 — 시드 스킵");
            return;
        }

        User master = User.create(
                MASTER_EMAIL,
                passwordEncoder.encode(MASTER_RAW_PASSWORD),
                MASTER_NAME,
                null,            // phone 선택
                UserRole.MASTER
        );
        userRepository.save(master);

        log.info("[Seed] MASTER 계정 생성 완료 — email={}, 초기 비밀번호={} (운영에선 즉시 변경 필요)",
                MASTER_EMAIL, MASTER_RAW_PASSWORD);
    }
}