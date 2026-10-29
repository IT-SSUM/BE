package com.itssum.domain.chat.controller;

import com.itssum.domain.chat.dto.request.SendChatMessageRequest;
import com.itssum.domain.chat.service.ChatMessageService;
import com.itssum.domain.chat.service.ChatUserResolver;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatUserResolver userResolver;
    private final ChatMessageService messageService;

    @MessageMapping("/chat-rooms/{chatRoomId}/messages")
    public void send(@DestinationVariable Long chatRoomId,
                     @Valid @Payload SendChatMessageRequest request,
                     Principal principal) {
        messageService.send(chatRoomId, userResolver.requireUser(principal), request);
    }
}
