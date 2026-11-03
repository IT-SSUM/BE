package com.itssum.domain.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateMatchQuantityRequest(
        @Schema(description = "변경할 합의 수량", example = "3")
        @NotNull @Positive Integer quantity
) {
}
