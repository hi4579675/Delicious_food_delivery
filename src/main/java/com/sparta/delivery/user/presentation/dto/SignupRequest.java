package com.sparta.delivery.user.presentation.dto;

import com.sparta.delivery.user.domain.entity.UserRole;
import jakarta.validation.constraints.*;

public record SignupRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8자 이상 15자 이하입니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=]).+$",
                message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "유저네임은 필수입니다.")
        @Size(min = 4, max = 10, message = "유저네임은 4자 이상 10자 이하입니다.")
        @Pattern(regexp = "^[a-z0-9]+$", message = "이름은 알파벳 소문자와 숫자만 가능합니다.")
        String name,

        String phone,

        @NotNull(message = "역할은 필수입니다.")
        UserRole role    // CUSTOMER, OWNER, MANAGER, MASTER
) {
        public SignupRequest {
                phone = (phone == null || phone.isBlank()) ? null : phone.strip();
        }

}