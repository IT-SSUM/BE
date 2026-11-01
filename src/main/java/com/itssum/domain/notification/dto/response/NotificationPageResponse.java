package com.itssum.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최신순 알림 내역")
public record NotificationPageResponse(
        List<NotificationResponse> notifications,
        @Schema(description = "다음 조회에 전달할 알림 ID. 더 이전 알림이 없으면 null") Long nextCursor,
        boolean hasNext
) {
}
