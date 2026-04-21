package com.sparta.delivery.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "유저네임은 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}