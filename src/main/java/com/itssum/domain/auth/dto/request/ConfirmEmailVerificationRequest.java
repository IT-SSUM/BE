package com.itssum.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "이메일 인증번호 확인 요청")
public record ConfirmEmailVerificationRequest(
        @Schema(description = "인증번호를 받은 이메일", example = "donor@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "메일로 받은 6자리 인증번호", example = "123456")
        @NotBlank @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자입니다.") String code
) {
}
