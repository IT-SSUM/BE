package com.itssum.domain.chat.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /** DB 저장이 확정된 메시지만 구독자에게 보낸다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ChatMessageSentEvent event) {
        Long roomId = event.message().chatRoomId();
        messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId, event.message());
    }
}
