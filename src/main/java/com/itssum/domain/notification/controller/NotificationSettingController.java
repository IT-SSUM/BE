package com.itssum.domain.notification.controller;

import com.itssum.domain.notification.dto.request.RegisterPushRegistrationRequest;
import com.itssum.domain.notification.dto.request.UpdateNotificationSettingRequest;
import com.itssum.domain.notification.dto.response.NotificationSettingResponse;
import com.itssum.domain.notification.dto.response.PushRegistrationResponse;
import com.itssum.domain.notification.service.NotificationSettingService;
import com.itssum.domain.notification.service.NotificationUserResolver;
import com.itssum.domain.notification.service.PushRegistrationService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "매칭·채팅 알림 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService settingService;
    private final PushRegistrationService pushRegistrationService;
    private final NotificationUserResolver userResolver;

    @Operation(summary = "알림 설정 조회", description = "매칭·채팅 기기 푸시 수신 설정을 조회합니다.")
    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> getSettings(Principal principal) {
        return ApiResponse.ok(SuccessCode.NOTIFICATION_SETTINGS_FOUND,
                settingService.get(userResolver.requireUser(principal).getId()));
    }

    @Operation(summary = "알림 설정 수정",
            description = "기기 푸시 수신 여부만 변경합니다. 앱 내 알림 기록과 WebSocket 전달은 유지됩니다.")
    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> updateSettings(
            @RequestBody UpdateNotificationSettingRequest request, Principal principal) {
        return ApiResponse.ok(SuccessCode.NOTIFICATION_SETTINGS_UPDATED,
                settingService.update(userResolver.requireUser(principal).getId(), request));
    }

    @Operation(summary = "기기 푸시 등록",
            description = "Firebase 설치 ID(FID)를 등록합니다. 같은 ID를 다시 등록하면 기기 유형을 갱신하고 현재 계정으로 연결합니다.")
    @PostMapping("/push-registrations")
    public ApiResponse<PushRegistrationResponse> registerPush(
            @Valid @RequestBody RegisterPushRegistrationRequest request, Principal principal) {
        return ApiResponse.ok(SuccessCode.PUSH_REGISTRATION_REGISTERED,
                pushRegistrationService.register(userResolver.requireUser(principal).getId(), request));
    }

    @Operation(summary = "기기 푸시 등록 삭제",
            description = "현재 계정에 등록된 기기만 삭제할 수 있습니다. 이 브라우저에서 로그아웃할 때 먼저 호출합니다.")
    @DeleteMapping("/push-registrations/{pushRegistrationId}")
    public ApiResponse<Void> deletePush(@PathVariable Long pushRegistrationId, Principal principal) {
        pushRegistrationService.delete(userResolver.requireUser(principal).getId(), pushRegistrationId);
        return ApiResponse.ok(SuccessCode.PUSH_REGISTRATION_DELETED);
    }
}
