package com.itssum.domain.notification.dto.response;

import com.itssum.domain.notification.entity.Notification;
import com.itssum.domain.notification.entity.NotificationTargetType;
import com.itssum.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "알림 정보")
public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        NotificationTargetType targetType,
        Long targetId,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(),
                notification.getTitle(), notification.getContent(), notification.getTargetType(),
                notification.getTargetId(), notification.isRead(), notification.getReadAt(),
                notification.getCreatedAt());
    }
}
