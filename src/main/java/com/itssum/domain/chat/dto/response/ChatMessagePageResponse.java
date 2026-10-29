package com.itssum.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "채팅 메시지 내역")
public record ChatMessagePageResponse(
        @Schema(description = "최신순 메시지 목록") List<ChatMessageResponse> messages,
        @Schema(description = "다음 조회의 beforeId. 더 이전 메시지가 없으면 null", example = "103") Long nextCursor,
        @Schema(description = "더 이전 메시지 존재 여부", example = "true") boolean hasNext,
        @Schema(description = "상대방이 마지막으로 읽은 메시지 ID. 아직 읽지 않았다면 null", example = "103")
        Long otherLastReadMessageId
) {
}
