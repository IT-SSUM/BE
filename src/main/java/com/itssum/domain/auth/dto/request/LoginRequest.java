package com.itssum.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 (기부자·기관 공통)")
public record LoginRequest(
        @Schema(description = "로그인 이메일", example = "donor@example.com")
        @NotBlank @Email String email,
        @Schema(description = "비밀번호", example = "abcd1234")
        @NotBlank String password
) {
}
