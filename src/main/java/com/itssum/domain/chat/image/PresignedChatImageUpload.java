package com.itssum.domain.chat.image;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "S3 채팅 이미지 업로드 정보")
public record PresignedChatImageUpload(
        @Schema(description = "업로드 후 IMAGE 메시지 content로 보내는 식별자",
                example = "chat/12/550e8400-e29b-41d4-a716-446655440000.jpg")
        String objectKey,
        @Schema(description = "10분간 유효한 S3 PUT 업로드 URL") String uploadUrl,
        @Schema(description = "S3 업로드 요청에 포함할 헤더") Map<String, String> requiredHeaders
) {
}
