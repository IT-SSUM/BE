package com.itssum.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅 메시지. SYSTEM 연결 안내에는 systemDescription이 포함됩니다")
public record ChatMessageResponse(
        @Schema(description = "메시지 ID", example = "105") Long messageId,
        @Schema(description = "채팅방 ID", example = "12") Long chatRoomId,
        @Schema(description = "보낸 사용자 ID. 시스템 메시지면 null", example = "34") Long senderId,
        @Schema(description = "메시지 유형(TEXT, IMAGE, SYSTEM, APPOINTMENT)", example = "TEXT")
        MessageType messageType,
        @Schema(description = "텍스트 또는 시스템 안내 제목. 이미지면 임시 조회 URL", example = "안녕하세요")
        String content,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "연결 안내 SYSTEM 메시지의 설명", example = "전달 날짜·장소·방법을 함께 정해 주세요.")
        String systemDescription,
        @Schema(description = "메시지 생성 시각", example = "2026-10-29T18:30:00")
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message, String displayContent) {
        return from(message, displayContent, null);
    }

    public static ChatMessageResponse from(ChatMessage message, String displayContent, String systemDescription) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender() == null ? null : message.getSender().getId(),
                message.getMessageType(),
                displayContent,
                systemDescription,
                message.getCreatedAt()
        );
    }
}
