package com.itssum.domain.chat.config;

import com.itssum.domain.chat.service.ChatAccessService;
import com.itssum.domain.chat.service.ChatUserResolver;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatStompInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_TOPIC = Pattern.compile("/topic/chat-rooms/(\\d+)(?:/read)?");
    private final ChatUserResolver userResolver;
    private final ChatAccessService accessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
        StompCommand command = headers.getCommand();
        if (command == StompCommand.CONNECT) {
            userResolver.requireUser(headers.getUser());
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = headers.getDestination();
            if (destination != null && destination.startsWith("/topic/chat-rooms/")) {
                Matcher matcher = CHAT_TOPIC.matcher(destination);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("잘못된 채팅 구독 경로입니다.");
                }
                Principal principal = headers.getUser();
                Long userId = userResolver.requireUser(principal).getId();
                accessService.requireRoomForParticipant(Long.valueOf(matcher.group(1)), userId);
            }
        }
        return message;
    }
}
