package com.sparta.delivery.user.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "p_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 10)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(nullable = false)
    private int tokenVersion = 0;

    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private boolean useAiDescription = false; // 기본값: 비활성

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String password, String name, String phone, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
    }

    /**
     * 외부 생성 진입점 — Service 에서만 사용
     */
    public static User create(String email, String encodedPassword,
                              String name, String phone, UserRole role) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .phone(phone)
                .role(role)
                .build();
    }

    public void updateInfo(String name, String phone, boolean isPublic, boolean useAiDescription) {
        this.name = name;
        this.phone = phone;
        this.isPublic = isPublic;
        this.useAiDescription = useAiDescription;
    }

    /**
     * 비밀번호 변경 시 기존 토큰 무효화
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        incrementTokenVersion();
    }

    /**
     * 역할 변경 시 기존 토큰 무효화 (OWNER→CUSTOMER 인수인계 시나리오 대응)
     */
    public void changeRole(UserRole role) {
        this.role = role;
        incrementTokenVersion();
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 권한 변경/탈퇴/비밀번호 변경 시 호출.
     * Filter 가 토큰의 tokenVersion 과 DB 값이 다르면 인증 거부.
     */
    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    /**
     * 탈퇴 처리.
     * - 이메일 치환으로 동일 이메일 재가입 허용
     * - BaseEntity.softDelete() 호출 → deletedAt/deletedBy 기록
     * - tokenVersion 증가로 기존 토큰 무효화
     */
    public void withdraw() {
        this.email = "withdrawn_" + this.userId + "@deleted";
        super.softDelete(this.userId);
        incrementTokenVersion();
    }
}