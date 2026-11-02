package com.itssum.global.infra.fcm;

import com.itssum.domain.notification.dto.response.NotificationResponse;

public interface PushSender {

    PushDeliveryResult send(String fid, NotificationResponse notification);
}
