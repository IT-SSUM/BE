package com.itssum.domain.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificationCreatedEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(event.receiverId()),
                    "/queue/notifications", event.notification());
        } catch (MessagingException exception) {
            log.warn("실시간 알림 전송에 실패했습니다. receiverId={}", event.receiverId(), exception);
        }
    }
}
