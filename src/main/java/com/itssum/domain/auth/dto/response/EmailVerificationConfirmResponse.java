package com.itssum.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailVerificationConfirmResponse(
        @Schema(description = "회원가입 요청에 넣는 이메일 인증 토큰 (30분, 1회용)")
        String emailVerificationToken
) {
}
