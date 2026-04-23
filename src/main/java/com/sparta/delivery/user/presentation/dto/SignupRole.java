package com.sparta.delivery.user.presentation.dto;

import com.sparta.delivery.user.domain.entity.UserRole;

/**
 * 회원가입에서 허용되는 역할만 노출.
 * MANAGER/MASTER 자가등록을 방지하기 위해 UserRole을 그대로 받지 않음.
 */
public enum SignupRole {
    CUSTOMER, OWNER;

    public UserRole toDomain() {
        return UserRole.valueOf(this.name());
    }
}