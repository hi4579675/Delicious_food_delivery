package com.sparta.delivery.user.presentation.dto;

import com.sparta.delivery.user.domain.entity.User;
import com.sparta.delivery.user.domain.entity.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String name,
        String phone,
        UserRole role,
        boolean isPublic,
        boolean useAiDescription,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.isPublic(),
                user.isUseAiDescription(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}