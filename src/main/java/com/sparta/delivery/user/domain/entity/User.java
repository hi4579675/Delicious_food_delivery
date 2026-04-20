package com.sparta.delivery.user.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티.
 * PK는 BIGINT (user_id) — 과제 원문은 username이 PK였으나 튜터가 별 지적 안함.
 * 로그인 식별자는 email 사용.
 *
 * BaseEntity 상속으로 감사 필드(createdAt/By, updatedAt/By, deletedAt/By) 자동 관리.
 * 다른 도메인은 User를 @ManyToOne이 아니라 Long userId FK로만 참조할 것.
 */
@Entity
@Table(name = "p_user")
@SQLRestriction("deleted_at IS NULL") // 모든 쿼리에 deleted_at IS NULL 자동 추가 (soft delete)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 20)
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

    // AI 상품 설명 기능에서 사용할 사용자 선호 텍스트
    @Column(columnDefinition = "TEXT")
    private String useAiDescription;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String password, String name, String phone, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
    }

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
    public void updateInfo(String name, String phone, boolean isPublic, String useAiDescription) {
        this.name = name;
        this.phone = phone;
        this.isPublic = isPublic;
        this.useAiDescription = useAiDescription;
    }

    public void changeRole(UserRole role) {
        this.role = role;
        incrementTokenVersion();
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 권한 변경/탈퇴/로그아웃 시 호출. 기존 JWT를 무효화한다.
     * Filter가 토큰의 tokenVersion과 DB값이 다르면 인증을 거부.
     */
    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    /**
     * 탈퇴 처리. soft delete + 이메일 무효화로 동일 이메일 재가입 허용.
     * BaseEntity.softDelete() 를 호출해 deletedAt/deletedBy 기록.
     */
    public void withdraw() {
        this.email = "withdrawn_" + this.userId + "@deleted";
        super.softDelete(this.userId);   // 본인이 탈퇴하니 deletedBy = 본인
        incrementTokenVersion();         // 기존 토큰 무효화
    }
}
