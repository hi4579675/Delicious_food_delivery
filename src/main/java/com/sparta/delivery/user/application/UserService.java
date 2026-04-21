package com.sparta.delivery.user.application;

import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.entity.UserRole;
import com.sparta.delivery.user.domain.exception.DuplicateEmailException;
import com.sparta.delivery.user.domain.exception.ForbiddenRoleChangeException;
import com.sparta.delivery.user.domain.exception.InvalidPasswordException;
import com.sparta.delivery.user.domain.exception.UserNotFoundException;
import com.sparta.delivery.user.domain.repository.UserRepository;
import com.sparta.delivery.user.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입.
     * SignupRole -> UserRole 변환 (SignupRole은 CUSTOMER/OWNER 만 있어 MANAGER/MASTER 자가 등록 원천 차단)
     */
    @Transactional
    public Long signUp(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone(),
                request.role().toDomain()
        );
        return userRepository.save(user).getUserId();
    }

    /** 내 정보 조회 */
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    /** 내 정보 수정 */
    @Transactional
    public void updateInfo(Long userId, UserUpdateRequest request) {
        User user = findUser(userId);
        user.updateInfo(
                request.name(),
                request.phone(),
                request.isPublic(),
                request.useAiDescription()
        );
    }

    /** 비밀번호 변경 */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 탈퇴.
     *
     * 엔티티의 withdraw() 가 다음을 수행:
     *  - 이메일을 withdrawn_{userId}@deleted 로 치환 (동일 이메일 재가입 허용)
     *  - BaseEntity.softDelete() 로 deletedAt/deletedBy 기록
     *  - tokenVersion 증가로 기존 토큰 무효화
     * 탈퇴 경로는 반드시 이 메서드만 사용할 것.
     */
    @Transactional
    public void withdraw(Long userId) {
        findUser(userId).withdraw();
    }


    /**
     * 역할 변경 (관리자 기능).
     *
     * 인가 규칙:
     *  - MASTER : 모든 대상에 대해 모든 role 로 변경 가능
     *  - MANAGER: MANAGER/MASTER 를 "생성" 또는 "대상으로 변경" 불가
     *    → MANAGER/MASTER 를 건드리거나, 누구를 MANAGER/MASTER 로 만드는 시도 모두 차단
     *
     * actor(요청자) 정보는 Controller 의 @AuthenticationPrincipal 에서 꺼내 전달받는다.
     * @PreAuthorize 만으로는 role 매트릭스를 표현할 수 없어 Service 에서 추가 검증.
     */
    @Transactional
    public void changeRole(Long targetUserId, RoleChangeRequest request, Long actorId) {
        User actor = findUser(actorId);
        User target = findUser(targetUserId);
        UserRole newRole = request.role();

        if (actor.getRole() == UserRole.MANAGER) {
            boolean touchingPrivilegedTarget =
                    target.getRole() == UserRole.MANAGER || target.getRole() == UserRole.MASTER;
            boolean promotingToPrivileged =
                    newRole == UserRole.MANAGER || newRole == UserRole.MASTER;

            if (touchingPrivilegedTarget || promotingToPrivileged) {
                throw new ForbiddenRoleChangeException();
            }
        }
        target.changeRole(newRole);
    }

    /** 로그인 성공 시 lastLoginAt 갱신. */
    @Transactional
    public void updateLastLoginAt(Long userId) {
        findUser(userId).updateLastLoginAt();
    }

    /** email로 유저 조회. */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * 공통메서드 : userId 로 활성 사용자 조회.
     *
     * @SQLRestriction("deleted_at IS NULL") 이 엔티티 레벨에 걸려있어
     * 탈퇴한 유저는 자동으로 Optional.empty() 가 되고 UserNotFoundException 으로 올라간다.
     * 모든 쓰기/읽기 메서드가 이 헬퍼를 거쳐서 중복 제거 + 동일한 에러 응답 보장.
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
