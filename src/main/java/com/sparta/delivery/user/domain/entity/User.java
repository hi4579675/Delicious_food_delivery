package com.sparta.delivery.user.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Entity
@Table(name = "p_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE p_user SET deleted_at = NOW() WHERE user_id = ?")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String name;


    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(nullable = false)
    private int tokenVersion = 0;

    private LocalDateTime lastLoginAt;

    @Column(columnDefinition = "TEXT")
    private String useAiDescription;

    @Builder
    public User(String email, String password, String name, String phone, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
    }

    public void updateInfo(String name, String phone, boolean isPublic, String useAiDescription) {
        this.name = name;
        this.phone = phone;
        this.isPublic = isPublic;
        this.useAiDescription = useAiDescription;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        incrementTokenVersion();
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

}
