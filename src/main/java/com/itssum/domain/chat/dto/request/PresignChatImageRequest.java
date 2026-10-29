package com.itssum.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅 이미지 업로드 URL 발급 요청")
public record PresignChatImageRequest(
        @Schema(description = "이미지 MIME 타입(image/jpeg, image/png, image/webp)", example = "image/jpeg")
        @NotBlank String contentType
) {
}
