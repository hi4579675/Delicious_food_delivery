package com.sparta.delivery.address.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
        @Size(max = 50, message = "배송지 별칭은 50자 이하여야 합니다.")
        String alias,

        @NotBlank(message = "주소는 필수 입력값입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        String detail,

        @Size(max = 10, message = "우편번호는 10자 이하여야 합니다.")
        String zipCode
) {
}
