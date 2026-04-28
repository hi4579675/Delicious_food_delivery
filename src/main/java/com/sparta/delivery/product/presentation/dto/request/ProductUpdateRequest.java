package com.sparta.delivery.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String productName,

        @NotNull
        Integer price,

        String description,

        @PositiveOrZero
        Integer displayOrder
) {
}
