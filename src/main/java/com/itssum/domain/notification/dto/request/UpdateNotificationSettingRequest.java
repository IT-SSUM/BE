package com.itssum.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기기 푸시 수신 설정. 생략한 항목은 기존 값을 유지합니다.")
public record UpdateNotificationSettingRequest(
        @Schema(description = "매칭 기기 푸시 수신 여부") Boolean matchEnabled,
        @Schema(description = "채팅 기기 푸시 수신 여부") Boolean chatEnabled
) {
}
