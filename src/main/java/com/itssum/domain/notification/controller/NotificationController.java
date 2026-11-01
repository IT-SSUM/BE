package com.itssum.domain.notification.controller;

import com.itssum.domain.notification.dto.response.NotificationPageResponse;
import com.itssum.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.itssum.domain.notification.service.NotificationService;
import com.itssum.domain.notification.service.NotificationUserResolver;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "서비스 알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationUserResolver userResolver;

    @Operation(summary = "알림 목록 조회", description = "내 알림을 최신순으로 조회합니다. 다음 조회에는 nextCursor를 beforeId로 전달합니다.")
    @GetMapping
    public ApiResponse<NotificationPageResponse> list(
            @Parameter(description = "이 ID보다 이전 알림을 조회합니다") @RequestParam(required = false) Long beforeId,
            @Parameter(description = "조회 개수(1~100, 기본 30)") @RequestParam(defaultValue = "30") int size,
            Principal principal) {
        return ApiResponse.ok(SuccessCode.NOTIFICATION_LIST_FOUND,
                notificationService.list(userResolver.requireUser(principal).getId(), beforeId, size));
    }

    @Operation(summary = "안 읽은 알림 수 조회")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> unreadCount(Principal principal) {
        return ApiResponse.ok(SuccessCode.NOTIFICATION_UNREAD_COUNT_FOUND,
                notificationService.unreadCount(userResolver.requireUser(principal).getId()));
    }

    @Operation(summary = "알림 읽음 처리", description = "내 알림만 읽음 처리할 수 있습니다. 이미 읽은 알림은 그대로 둡니다.")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long notificationId, Principal principal) {
        notificationService.markRead(notificationId, userResolver.requireUser(principal).getId());
        return ApiResponse.ok(SuccessCode.NOTIFICATION_READ);
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "현재 안 읽은 내 알림을 모두 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(userResolver.requireUser(principal).getId());
        return ApiResponse.ok(SuccessCode.NOTIFICATION_ALL_READ);
    }
}
