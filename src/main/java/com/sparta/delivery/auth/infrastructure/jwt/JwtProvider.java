package com.sparta.delivery.auth.infrastructure.jwt;

import com.sparta.delivery.auth.domain.exception.InvalidTokenException;
import com.sparta.delivery.auth.domain.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 생성/파싱/검증.
 *
 * - 알고리즘: HS256 (docs/003-infrastructure.md)
 *  - Subject: userId (Long)
 *  - Claims: role(String), tv(tokenVersion, int)
 *
 * 보안 주의:
 *  - key 필드는 외부 노출 금지 (private, getter 없음)
 *  - secret.getBytes() 시 StandardCharsets.UTF_8 명시 (플랫폼 기본 인코딩 의존 회피)
 *  - 파싱 실패 시 원인별 예외 분리 (만료 vs 기타) — 프론트가 재로그인/재발급 분기 가능
 */
@Slf4j
@Component
public class JwtProvider {

    // claim key 는 상수로 — 오타 방지, 리팩터링 안전
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_VERSION = "tv";

    private final Key key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 액세스 토큰 발급.
     *
     * @param userId       사용자 PK (subject 로 저장)
     * @param role         "CUSTOMER" / "OWNER" / "MANAGER" / "MASTER"
     * @param tokenVersion 발급 시점의 DB tokenVersion — 강제 무효화 비교용
     */
    public String generateToken(Long userId, String role, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // sub = userId
                .claim(CLAIM_ROLE, role)            // role
                .claim(CLAIM_TOKEN_VERSION, tokenVersion) // tv
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 파싱 + 검증 + claim 추출을 한 번에 수행.
     *
     * 파싱 3회 분리(validate/getUsername/getRole 각각 호출) 패턴을 피함 — 1회 파싱으로 통합.
     *
     * 예외 매핑:
     *  - 만료 → TokenExpiredException (AUTH-002)
     *  - 서명 오류 / 포맷 오류 / 필수 claim 누락·타입 오류 / subject 숫자변환 실패 → InvalidTokenException (AUTH-003)
     *
     *  NPE/NumberFormatException 이 Filter 밖으로 새어나가 500 이 되는 경로를 모두 차단.
     */
    public TokenPayload parse(String token) {
        // 1단계 : 파싱/서명검증
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료는 별도 에러 코드로 - 프론트가 재로그인 유도 분기 가능
            throw new TokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 파싱 실패: {}", e.getMessage());
            throw new InvalidTokenException();
        }

        // 2단계 : claim 추출 + null/ 타입 가드
        try {
            String subject = claims.getSubject();
            String role = claims.get(CLAIM_ROLE, String.class);
            Integer tokenVersion = claims.get(CLAIM_TOKEN_VERSION, Integer.class);

            // 필수 claim 누락 방어 - 자체 발급 토큰이면 전부 있어야 함
            if (subject == null || role == null || tokenVersion == null) {
                log.debug("JWT 필수 claim 누락: subject={}, role={}, tv={}", subject, role, tokenVersion);
                throw new InvalidTokenException();
            }
            return new TokenPayload(Long.parseLong(subject), role, tokenVersion);
        } catch (NumberFormatException | JwtException e) {
            // NumberFormatException: subject 가 숫자가 아닐 때
            // JwtException(RequiredTypeException 포함): claim 타입 불일치
            log.debug("JWTO Claim 추출 실패 : {}", e.getMessage());
            throw new InvalidTokenException();
        }

    }

    /** AuthService.login 응답의 expiresIn 필드용 */
    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * 파싱 결과 값 객체.
     * 호출부(Filter) 가 Claims 를 직접 만지지 않게 감쌈.
     * record 라 equals/hashCode/toString 자동 — 테스트에서 비교 편함.
     */
    public record TokenPayload(Long userId, String role, int tokenVersion) {}

}
