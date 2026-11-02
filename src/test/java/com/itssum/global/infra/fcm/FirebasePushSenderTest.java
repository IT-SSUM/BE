package com.itssum.global.infra.fcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.itssum.domain.notification.dto.response.NotificationResponse;
import com.itssum.domain.notification.entity.NotificationTargetType;
import com.itssum.domain.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;

class FirebasePushSenderTest {

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FirebasePushSender sender = new FirebasePushSender(messaging);
    private final NotificationResponse notification = new NotificationResponse(
            1L, NotificationType.CHAT, "새 채팅 메시지", "새 메시지가 도착했어요.",
            NotificationTargetType.CHAT_ROOM, 2L, false, null, null);

    @Test
    void invalidArgumentRemovesRegistration() throws Exception {
        FirebaseMessagingException failure = mock(FirebaseMessagingException.class);
        when(failure.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        when(messaging.send(any(Message.class))).thenThrow(failure);

        assertEquals(PushDeliveryResult.INVALID_REGISTRATION, sender.send("fid", notification));
    }

    @Test
    void serviceUnavailableDoesNotRemoveRegistration() throws Exception {
        FirebaseMessagingException failure = mock(FirebaseMessagingException.class);
        when(failure.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(messaging.send(any(Message.class))).thenThrow(failure);

        assertEquals(PushDeliveryResult.FAILED, sender.send("fid", notification));
    }
}
