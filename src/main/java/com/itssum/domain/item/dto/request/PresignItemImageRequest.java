package com.itssum.domain.item.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "물품 사진 업로드 URL 발급 요청")
public record PresignItemImageRequest(
        @Schema(description = "이미지 MIME 타입(image/jpeg, image/png, image/webp)", example = "image/jpeg")
        @NotBlank String contentType
) {
}
