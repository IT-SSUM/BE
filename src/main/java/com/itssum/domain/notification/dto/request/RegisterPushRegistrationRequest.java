package com.itssum.domain.notification.dto.request;

import com.itssum.domain.notification.entity.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "기기 푸시 등록 요청")
public record RegisterPushRegistrationRequest(
        @Schema(description = "Firebase 설치 ID (FID)")
        @NotBlank @Size(max = 255) String fid,
        @NotNull DeviceType deviceType
) {
}
