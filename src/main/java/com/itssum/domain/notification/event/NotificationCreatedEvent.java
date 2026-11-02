package com.itssum.domain.notification.event;

import com.itssum.domain.notification.dto.response.NotificationResponse;

public record NotificationCreatedEvent(Long receiverId, NotificationResponse notification) {
}
