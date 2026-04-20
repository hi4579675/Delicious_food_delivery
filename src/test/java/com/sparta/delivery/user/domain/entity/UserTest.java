package com.sparta.delivery.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User 엔티티의 도메인 메서드 단위 테스트.
 * DB/Spring 없이 순수 POJO로 검증 → 빠름.
 */
class UserTest {

    /** 테스트용 기본 User 생성 헬퍼 */
    private User buildUser() {
        return User.builder()
                .email("test@test.com")
                .password("encoded-password")
                .name("tester")
                .phone("010-1234-5678")
                .role(UserRole.CUSTOMER)
                .build();
    }

    @Nested
    @DisplayName("빌더로 User를 생성하믄.")
    class CreateUser {

        @Test
        @DisplayName("전달한 값이 그대로 들어간다.")
        void build_setsFields() {
            // given
            User user = buildUser();

            assertThat(user.getEmail()).isEqualTo("test@test.com");
            assertThat(user.getName()).isEqualTo("tester");
            assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("isPublic 기본값은 true, tokenVersion 기본값은 0")
        void build_defaults() {
            User user = buildUser();

            assertThat(user.isPublic()).isTrue();
            assertThat(user.getTokenVersion()).isZero();
            assertThat(user.getLastLoginAt()).isNull();
        }
    }

    @Nested
    @DisplayName("changeRole 호출 시")
    class ChangeRole {

        @Test
        @DisplayName("role이 변경된다")
        void changesRole() {
            User user = buildUser();

            user.changeRole(UserRole.OWNER);

            assertThat(user.getRole()).isEqualTo(UserRole.OWNER);
        }
    }

    @Nested
    @DisplayName("incrementTokenVersion 호출 시")
    class IncrementTokenVersion {

        @Test
        @DisplayName("tokenVersion이 1 증가한다")
        void increments() {
            User user = buildUser();
            int before = user.getTokenVersion();

            user.incrementTokenVersion();

            assertThat(user.getTokenVersion()).isEqualTo(before + 1);
        }

        @Test
        @DisplayName("여러 번 호출하면 그 횟수만큼 증가한다")
        void multipleIncrements() {
            User user = buildUser();

            user.incrementTokenVersion();
            user.incrementTokenVersion();
            user.incrementTokenVersion();

            assertThat(user.getTokenVersion()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("softDelete 호출 시 (BaseEntity 로직)")
    class SoftDelete {

        @Test
        @DisplayName("deletedAt, deletedBy가 세팅된다")
        void setsDeletedFields() {
            User user = buildUser();

            user.softDelete(999L);

            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getDeletedBy()).isEqualTo(999L);
        }

        @Test
        @DisplayName("이미 삭제된 엔티티를 다시 삭제해도 값이 덮어쓰이지 않는다")
        void doesNotOverwriteIfAlreadyDeleted() {
            User user = buildUser();
            user.softDelete(1L);
            var firstDeletedAt = user.getDeletedAt();

            user.softDelete(2L);

            // deletedBy는 처음 삭제한 1L 유지
            assertThat(user.getDeletedBy()).isEqualTo(1L);
            assertThat(user.getDeletedAt()).isEqualTo(firstDeletedAt);
        }
    }

}