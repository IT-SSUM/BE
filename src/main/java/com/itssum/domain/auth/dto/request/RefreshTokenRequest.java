package com.itssum.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record RefreshTokenRequest(
        @Schema(description = "로그인·가입·재발급 응답의 refreshToken")
        @NotBlank String refreshToken
) {
}
