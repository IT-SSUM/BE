package com.itssum.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "채팅 메시지 읽음 처리 요청")
public record MarkChatReadRequest(
        @Schema(description = "마지막으로 읽은 채팅방 내 메시지 ID", example = "105")
        @Positive Long lastReadMessageId
) {
}
