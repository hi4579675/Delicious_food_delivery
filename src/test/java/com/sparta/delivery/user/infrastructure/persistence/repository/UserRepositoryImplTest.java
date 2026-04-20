package com.sparta.delivery.user.infrastructure.persistence.repository;

import com.sparta.delivery.common.config.jpa.JpaAuditingConfig;
import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 통합 테스트.
 *
 * @DataJpaTest: JPA 관련 빈만 로드 → Spring Boot 전체 컨텍스트보다 빠름
 * @AutoConfigureTestDatabase(replace = NONE): H2가 아니라 실제 DB 설정 사용
 * @Import(JpaAuditingConfig.class): BaseEntity의 @CreatedDate/By 자동화 활성화
 *
 * UserRepositoryImpl + UserJpaRepository를 함께 Bean으로 잡기 위해
 * @Import로 구현체를 추가한다.
 */
@DataJpaTest
@Import({UserRepositoryImpl.class, JpaAuditingConfig.class})
class UserRepositoryImplTest {
    @Autowired
    private UserRepositoryImpl userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @BeforeEach
    void setUp() {
        userJpaRepository.deleteAll();
    }

    private User newUser(String email, String name) {
        return User.builder()
                .email(email)
                .password("encoded")
                .name(name)
                .phone("010-0000-0000")
                .role(UserRole.CUSTOMER)
                .build();
    }

    @Nested
    @DisplayName("save / findById")
    class SaveAndFind {

        @Test
        @DisplayName("저장 후 ID로 조회할 수 있다")
        void save_findById() {
            User saved = userRepository.save(newUser("a@a.com", "userA"));

            Optional<User> found = userRepository.findById(saved.getUserId());

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("a@a.com");
        }

        @Test
        @DisplayName("없는 ID로 조회하면 Optional.empty")
        void findById_notFound() {
            Optional<User> found = userRepository.findById(999L);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmailAndDeletedAtIsNull")
    class FindByEmail {

        @Test
        @DisplayName("삭제되지 않은 사용자는 이메일로 조회된다")
        void returnsActiveUser() {
            userRepository.save(newUser("a@a.com", "userA"));

            Optional<User> found = userRepository.findByEmailAndDeletedAtIsNull("a@a.com");

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("Soft Delete된 사용자는 조회되지 않는다")
        void excludesDeletedUser() {
            User user = userRepository.save(newUser("a@a.com", "userA"));
            user.softDelete(user.getUserId());
            userJpaRepository.flush();  // deletedAt을 실제 DB에 반영

            Optional<User> found = userRepository.findByEmailAndDeletedAtIsNull("a@a.com");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("없는 이메일로 조회하면 Optional.empty")
        void notFound() {
            Optional<User> found = userRepository.findByEmailAndDeletedAtIsNull("none@none.com");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("저장된 이메일은 true")
        void exists() {
            userRepository.save(newUser("a@a.com", "userA"));

            assertThat(userRepository.existsByEmail("a@a.com")).isTrue();
        }

        @Test
        @DisplayName("저장되지 않은 이메일은 false")
        void notExists() {
            assertThat(userRepository.existsByEmail("none@none.com")).isFalse();
        }
    }
}