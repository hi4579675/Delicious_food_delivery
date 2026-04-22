package com.sparta.delivery.region.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegionCreateRequest(

        @NotBlank(message = "지역 코드는 필수입니다.")
        @Pattern(regexp = "^\\d{10}$", message = "지역 코드는 10자리 숫자여야 합니다.")
        String regionCode,

        @NotBlank(message = "지역명은 필수입니다.")
        @Size(max = 100, message = "지역명은 100자 이하여야 합니다.")
        String regionName,

        UUID parentId,

        @NotNull(message = "depth는 필수입니다.")
        Integer depth,

        @NotNull(message = "활성 여부는 필수입니다.")
        Boolean isActive
) {
}
