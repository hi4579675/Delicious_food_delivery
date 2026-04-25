package com.sparta.delivery.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequest(

        @NotNull(message = "평점 입력은 필수입니다.")
        @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5 이하이어야 합니다.")
        Integer rating,

        @Size(max = 500, message = "리뷰 내용은 500자 이하여야 합니다.")
        String content
) {

}
