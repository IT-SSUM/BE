package com.itssum.domain.notification.dto.response;

import com.itssum.domain.notification.entity.NotificationSetting;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매칭·채팅 기기 푸시 수신 설정")
public record NotificationSettingResponse(boolean matchEnabled, boolean chatEnabled) {

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(setting.isMatchEnabled(), setting.isChatEnabled());
    }
}
