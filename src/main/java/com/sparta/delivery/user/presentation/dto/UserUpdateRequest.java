package com.sparta.delivery.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50)
        String name,

        String phone,

        boolean isPublic,

        String useAiDescription
) {}