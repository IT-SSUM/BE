package com.itssum.domain.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMatchRequest(
        @Schema(description = "기부 물품 ID", example = "12")
        @NotNull @Positive Long donationItemId,
        @Schema(description = "필요 물품 ID", example = "25")
        @NotNull @Positive Long neededItemId,
        @Schema(description = "제안·요청 수량", example = "3")
        @Positive int quantity,
        @Schema(description = "상대에게 전할 메시지(선택)", example = "도서 세 권을 기부하고 싶습니다.")
        @Size(max = 500) String message
) {
}
