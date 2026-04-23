package com.sparta.delivery.user.presentation.dto;

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

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하입니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "이름은 한글, 영문, 숫자만 가능합니다 (공백/특수문자 불가).")
        String name,

        @Pattern(
                regexp = "^01\\d-?\\d{3,4}-?\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phone,

        @NotNull(message = "역할은 필수입니다.")
        SignupRole role    // CUSTOMER / OWNER 만 허용 — MANAGER/MASTER 자가등록 차단
) {
        public SignupRequest {
                phone = (phone == null || phone.isBlank()) ? null : phone.strip();
        }

}