package com.itssum.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "이메일 인증번호 발송 요청")
public record SendEmailVerificationRequest(
        @Schema(description = "인증할 이메일", example = "donor@example.com")
        @NotBlank @Email @Size(max = 255) String email
) {
}
