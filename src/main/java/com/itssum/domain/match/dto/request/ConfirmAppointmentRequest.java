package com.itssum.domain.match.dto.request;

import com.itssum.domain.match.entity.DeliveryMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record ConfirmAppointmentRequest(
        @Schema(description = "전달 방법", example = "DIRECT")
        @NotNull DeliveryMethod deliveryMethod,
        @Schema(description = "전달 예정 시각. 오프셋을 포함합니다", example = "2026-11-02T14:00:00+09:00")
        @NotNull @Future OffsetDateTime scheduledAt,
        @Schema(description = "전달 장소 또는 택배 수령 주소", example = "구로지역아동센터 입구")
        @NotBlank @Size(max = 255) String deliveryPlace,
        @Schema(description = "수령 담당자(선택)", example = "김담당")
        @Size(max = 30) String receiverName
) {
}
