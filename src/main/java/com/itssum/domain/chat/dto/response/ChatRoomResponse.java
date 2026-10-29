package com.itssum.domain.chat.dto.response;

import com.itssum.domain.match.entity.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅방 목록 항목. 대화 화면 상단 정보도 포함합니다")
public record ChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "12") Long chatRoomId,
        @Schema(description = "연결된 매칭 ID", example = "7") Long matchId,
        @Schema(description = "상대방 사용자 ID", example = "34") Long otherUserId,
        @Schema(description = "상대방 이름", example = "행복나눔센터") String otherName,
        @Schema(description = "연결된 기부 물품 ID", example = "123") Long donationItemId,
        @Schema(description = "기부 물품 이름", example = "어린이 도서") String itemName,
        @Schema(description = "이번 매칭의 거래 수량", example = "30") int quantity,
        @Schema(description = "물품 수량 단위", example = "권") String unit,
        @Schema(description = "매칭 상태", example = "RESERVED") MatchStatus matchStatus,
        @Schema(description = "최근 메시지 미리보기. 이미지면 [사진]", example = "내일 전달 가능할까요?")
        String lastMessageContent,
        @Schema(description = "최근 메시지 전송 시각", example = "2026-10-29T18:30:00")
        LocalDateTime lastMessageAt,
        @Schema(description = "내가 읽지 않은 메시지 수. 연결 안내는 제외", example = "2")
        long unreadCount
) {
}
