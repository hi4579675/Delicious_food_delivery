package com.sparta.delivery.user.presentation.dto;

import com.sparta.delivery.user.domain.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(

        @NotNull(message = "역할은 필수입니다.")
        UserRole role
) {}