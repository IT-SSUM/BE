package com.itssum.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailVerificationSendResponse(
        @Schema(description = "인증번호 유효시간(초)", example = "300") long expiresInSeconds
) {
}
