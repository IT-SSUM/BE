package com.itssum.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그아웃 요청. 이 기기의 리프레시 토큰만 폐기한다")
public record LogoutRequest(
        @Schema(description = "이 기기의 refreshToken")
        @NotBlank String refreshToken
) {
}
