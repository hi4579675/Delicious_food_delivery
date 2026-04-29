package com.sparta.delivery.product.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProductHiddenUpdateRequest(
        @NotNull
        Boolean hidden
) {
}
