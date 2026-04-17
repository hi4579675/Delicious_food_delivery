package com.sparta.delivery.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // abstract 라 어차피 직접 생성 불가지만 명시적으로
public abstract class BaseEntity {
    // ID 는 자식 엔티티에서 정의 (User: Long, 나머지: UUID)

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // nullable=false 의도적으로 제외:
    // 회원가입처럼 SecurityContext 가 비어있는 시점에 생성되는 엔티티(첫 유저/시드 데이터 등)는
    // AuditorAware 가 Optional.empty() 를 돌려주어 createdBy 가 null 로 들어갈 수 있음.
    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private Long updatedBy;

    private LocalDateTime deletedAt;
    private Long deletedBy;

    public void softDelete(Long userId) {
        if (isDeleted()) return; // 이미 삭제된 엔티티 재삭제 방지
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = userId;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}