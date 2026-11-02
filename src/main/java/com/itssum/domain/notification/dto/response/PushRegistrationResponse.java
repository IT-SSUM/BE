package com.itssum.domain.notification.dto.response;

import com.itssum.domain.notification.entity.DeviceType;
import com.itssum.domain.notification.entity.PushRegistration;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "등록된 기기 푸시 정보")
public record PushRegistrationResponse(Long pushRegistrationId, DeviceType deviceType) {

    public static PushRegistrationResponse from(PushRegistration registration) {
        return new PushRegistrationResponse(registration.getId(), registration.getDeviceType());
    }
}
