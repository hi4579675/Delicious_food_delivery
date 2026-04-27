package com.sparta.delivery.user.application;

import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.entity.UserRole;
import com.sparta.delivery.user.domain.exception.DuplicateEmailException;
import com.sparta.delivery.user.domain.exception.ForbiddenRoleChangeException;
import com.sparta.delivery.user.domain.exception.InvalidPasswordException;
import com.sparta.delivery.user.domain.exception.UserNotFoundException;
import com.sparta.delivery.user.domain.repository.UserRepository;
import com.sparta.delivery.user.presentation.dto.PasswordChangeRequest;
import com.sparta.delivery.user.presentation.dto.RoleChangeRequest;
import com.sparta.delivery.user.presentation.dto.SignupRequest;
import com.sparta.delivery.user.presentation.dto.SignupRole;
import com.sparta.delivery.user.presentation.dto.UserUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

     // 객체
     private User createUser(Long userId, String email, UserRole role, String encodedPassword) {
         User user = User.create(email, encodedPassword, "alice01", "01012345678", role);
         ReflectionTestUtils.setField(user, "userId", userId);
         return user;
     }

     // 회원가입
    @Nested
    @DisplayName("회원가입")
    class SignUp {
         @Test
         @DisplayName("정상 가입 - userId 반환, 비밀번호 인코딩, save 호출")
         void success() {
             // given
             SignupRequest req = new SignupRequest(
                     "alice@test.com", "Pass1234!", "alice01",
                     "01012345678", SignupRole.CUSTOMER
             );
             given(userRepository.existsByEmail("alice@test.com")).willReturn(false);
             given(passwordEncoder.encode("Pass1234!")).willReturn("ENCODED");
             given(userRepository.save(any(User.class)))
                     .willAnswer(inv -> {
                         User u = inv.getArgument(0);
                         ReflectionTestUtils.setField(u, "userId", 1L);
                         return u;
                     });

             // when
             Long userId = userService.signUp(req);

             // then
             assertThat(userId).isEqualTo(1L);
             verify(passwordEncoder).encode("Pass1234!");
             verify(userRepository).save(any(User.class));
         }

         @Test
         @DisplayName("이메일 중복이면 DuplicateEmailException")
         void duplicate_email() {
             // given
             SignupRequest req = new SignupRequest(
                     "alice@test.com", "Pass1234!", "alice01",
                     null, SignupRole.CUSTOMER
             );
             given(userRepository.existsByEmail("alice@test.com")).willReturn(true);

             // when / then
             assertThatThrownBy(() -> userService.signUp(req))
                     .isInstanceOf(DuplicateEmailException.class);
             verify(passwordEncoder, never()).encode(anyString());
             verify(userRepository, never()).save(any(User.class));
         }
     }

     // changePassword
    @Nested
    @DisplayName("비밀번호 변경")
     class ChangePassword {

         @Test
         @DisplayName("정상 변경 - password 교체 + tokenVersion 증가")
         void success() {
             User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "OLD_ENCODED");
             given(userRepository.findById(1L)).willReturn(Optional.of(user));
             given(passwordEncoder.matches("oldPw", "OLD_ENCODED")).willReturn(true);
             given(passwordEncoder.encode("NewPw123!")).willReturn("NEW_ENCODED");

             userService.changePassword(1L, new PasswordChangeRequest("oldPw", "NewPw123!"));

             assertThat(user.getPassword()).isEqualTo("NEW_ENCODED");
             assertThat(user.getTokenVersion()).isEqualTo(1);
             verify(userRepository, never()).save(any());   // dirty checking 기대
         }

         @Test
         @DisplayName("현재 비밀번호 불일치 시 InvalidPasswordException")
         void wrong_current_password() {
             User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "OLD_ENCODED");
             given(userRepository.findById(1L)).willReturn(Optional.of(user));
             given(passwordEncoder.matches("wrong", "OLD_ENCODED")).willReturn(false);

             assertThatThrownBy(() ->
                     userService.changePassword(1L, new PasswordChangeRequest("wrong", "NewPw123!")))
                     .isInstanceOf(InvalidPasswordException.class);
             verify(passwordEncoder, never()).encode(anyString());
         }
     }

     // 탈퇴
     @Nested
     @DisplayName("탈퇴")
     class Withdraw {

         @Test
         @DisplayName("이메일 치환 + deletedAt 세팅 + tokenVersion 증가")
         void success() {
             User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "ENCODED");
             given(userRepository.findById(1L)).willReturn(Optional.of(user));

             userService.withdraw(1L);

             assertThat(user.getEmail()).isEqualTo("withdrawn_1@deleted");
             assertThat(user.getDeletedAt()).isNotNull();
             assertThat(user.getDeletedBy()).isEqualTo(1L);
             assertThat(user.getTokenVersion()).isEqualTo(1);
         }
     }

    // 내정보 수정

    @Nested
    @DisplayName("내 정보 수정")
    class UpdateInfo {

        @Test
        @DisplayName("name/phone/isPublic/useAiDescription 변경")
        void success() {
            User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "ENCODED");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            userService.updateInfo(1L,
                    new UserUpdateRequest("bob02", "01099998888", false, true));

            assertThat(user.getName()).isEqualTo("bob02");
            assertThat(user.getPhone()).isEqualTo("01099998888");
            assertThat(user.isPublic()).isFalse();
            assertThat(user.isUseAiDescription()).isTrue();
            verify(userRepository, never()).save(any());
        }
    }

    // 권한
    @Nested
    @DisplayName("역할 변경 권한 매트릭스")
    class ChangeRole {

        @Test
        @DisplayName("MASTER 는 CUSTOMER 를 MANAGER 로 승격 가능")
        void master_can_promote_to_manager() {
            User master = createUser(1L, "m@test.com", UserRole.MASTER, "E");
            User target = createUser(2L, "c@test.com", UserRole.CUSTOMER, "E");
            given(userRepository.findById(1L)).willReturn(Optional.of(master));
            given(userRepository.findById(2L)).willReturn(Optional.of(target));

            userService.changeRole(2L, new RoleChangeRequest(UserRole.MANAGER), 1L);

            assertThat(target.getRole()).isEqualTo(UserRole.MANAGER);
        }

        @Test
        @DisplayName("MANAGER 가 MANAGER 를 건드리면 ForbiddenRoleChangeException")
        void manager_cannot_touch_manager() {
            User manager = createUser(1L, "mgr@test.com", UserRole.MANAGER, "E");
            User target = createUser(2L, "other@test.com", UserRole.MANAGER, "E");
            given(userRepository.findById(1L)).willReturn(Optional.of(manager));
            given(userRepository.findById(2L)).willReturn(Optional.of(target));

            assertThatThrownBy(() ->
                    userService.changeRole(2L, new RoleChangeRequest(UserRole.CUSTOMER), 1L))
                    .isInstanceOf(ForbiddenRoleChangeException.class);
            assertThat(target.getRole()).isEqualTo(UserRole.MANAGER); // 변화 없음
        }

        @Test
        @DisplayName("MANAGER 가 CUSTOMER 를 MANAGER 로 승격하려 하면 ForbiddenRoleChangeException")
        void manager_cannot_promote_to_manager() {
            User manager = createUser(1L, "mgr@test.com", UserRole.MANAGER, "E");
            User target = createUser(2L, "cst@test.com", UserRole.CUSTOMER, "E");
            given(userRepository.findById(1L)).willReturn(Optional.of(manager));
            given(userRepository.findById(2L)).willReturn(Optional.of(target));

            assertThatThrownBy(() ->
                    userService.changeRole(2L, new RoleChangeRequest(UserRole.MANAGER), 1L))
                    .isInstanceOf(ForbiddenRoleChangeException.class);
        }

        @Test
        @DisplayName("MANAGER 가 CUSTOMER 를 OWNER 로 변경은 가능")
        void manager_can_change_customer_to_owner() {
            User manager = createUser(1L, "mgr@test.com", UserRole.MANAGER, "E");
            User target = createUser(2L, "cst@test.com", UserRole.CUSTOMER, "E");
            given(userRepository.findById(1L)).willReturn(Optional.of(manager));
            given(userRepository.findById(2L)).willReturn(Optional.of(target));

            userService.changeRole(2L, new RoleChangeRequest(UserRole.OWNER), 1L);

            assertThat(target.getRole()).isEqualTo(UserRole.OWNER);
        }

        @Test
        @DisplayName("MASTER 가 본인을 강등하면 ForbiddenRoleChangeException")
        void master_cannot_demote_self() {
            User master = createUser(1L, "m@test.com", UserRole.MASTER, "E");
            given(userRepository.findById(1L)).willReturn(Optional.of(master));

            assertThatThrownBy(() ->
                    userService.changeRole(1L, new RoleChangeRequest(UserRole.MANAGER), 1L))
                    .isInstanceOf(ForbiddenRoleChangeException.class);
            assertThat(master.getRole()).isEqualTo(UserRole.MASTER);   // 변화 없음
        }
    }

    // 내정보 조회
    @Nested
    @DisplayName("내 정보 조회")
    class getMe {

         @Test
         @DisplayName("정상 조회")
        void success() {
             User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "E");
             given(userRepository.findById(1L)).willReturn(Optional.of(user));

             var res =  userService.getMe(1L);
             assertThat(res.userId()).isEqualTo(1L);
             assertThat(res.email()).isEqualTo("alice@test.com");
         }

    }

    // 이메일
    @Nested
    @DisplayName("email 로 사용자 조회")
    class FindByEmail {

        @Test
        @DisplayName("정상 조회")
        void success() {
            User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "E");
            given(userRepository.findByEmail("alice@test.com")).willReturn(Optional.of(user));

            User found = userService.findByEmail("alice@test.com");

            assertThat(found.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("없으면 UserNotFoundException")
        void not_found() {
            given(userRepository.findByEmail("no@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("no@test.com"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // 강제 로그아웃 (tokenVersion 증가)
    @Nested
    @DisplayName("강제 로그아웃 (forceLogout)")
    class ForceLogout {

        @Test
        @DisplayName("정상 호출 - tokenVersion 1 증가")
        void success() {
            // given : tv=3 인 유저
            User user = createUser(1L, "alice@test.com", UserRole.CUSTOMER, "ENCODED");
            ReflectionTestUtils.setField(user, "tokenVersion", 3);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            userService.forceLogout(1L);

            // then : 동일 객체 상태로 확인 (도메인 메서드가 처리)
            assertThat(user.getTokenVersion()).isEqualTo(4);
        }

        @Test
        @DisplayName("존재하지 않는 유저 - UserNotFoundException")
        void user_not_found() {
            // given : findById 가 empty
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.forceLogout(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}