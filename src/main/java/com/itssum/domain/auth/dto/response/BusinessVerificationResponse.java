package com.itssum.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record BusinessVerificationResponse(
        @Schema(description = "기관 회원가입 요청에 넣는 사업자 확인 토큰 (30분, 1회용)")
        String businessVerificationToken
) {
}
