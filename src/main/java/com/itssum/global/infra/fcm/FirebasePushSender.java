package com.itssum.global.infra.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.itssum.domain.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "push.firebase.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FirebasePushSender implements PushSender {

    private final FirebaseMessaging messaging;

    @Override
    public PushDeliveryResult send(String fid, NotificationResponse notification) {
        Message.Builder message = Message.builder()
                .setFid(fid)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.title())
                        .setBody(notification.content() == null ? "" : notification.content())
                        .build())
                .putData("notificationId", notification.notificationId().toString())
                .putData("type", notification.type().name());
        if (notification.targetType() != null && notification.targetId() != null) {
            message.putData("targetType", notification.targetType().name())
                    .putData("targetId", notification.targetId().toString());
        }
        try {
            messaging.send(message.build());
            return PushDeliveryResult.SENT;
        } catch (FirebaseMessagingException exception) {
            if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || exception.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                return PushDeliveryResult.INVALID_REGISTRATION;
            }
            log.warn("FCM 전송에 실패했습니다. notificationId={}, errorCode={}",
                    notification.notificationId(), exception.getMessagingErrorCode(), exception);
            return PushDeliveryResult.FAILED;
        }
    }
}
