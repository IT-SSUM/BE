package com.itssum.domain.chat.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.service.ChatAccessService;
import com.itssum.domain.chat.service.ChatUserResolver;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class ChatStompInterceptorTest {

    private final ChatUserResolver userResolver = mock(ChatUserResolver.class);
    private final ChatAccessService accessService = mock(ChatAccessService.class);
    private final ChatStompInterceptor interceptor = new ChatStompInterceptor(userResolver, accessService);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void chatSubscriptionChecksParticipant() {
        Principal principal = () -> "10";
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(userResolver.requireUser(principal)).thenReturn(user);
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setDestination("/topic/chat-rooms/42");
        headers.setUser(principal);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        interceptor.preSend(message, channel);

        verify(accessService).requireRoomForParticipant(42L, 10L);
    }

    @Test
    void readReceiptSubscriptionChecksParticipant() {
        Principal principal = () -> "10";
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(userResolver.requireUser(principal)).thenReturn(user);
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setDestination("/topic/chat-rooms/42/read");
        headers.setUser(principal);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        interceptor.preSend(message, channel);

        verify(accessService).requireRoomForParticipant(42L, 10L);
    }

    @Test
    void otherSubscriptionsAreRejected() {
        for (String destination : new String[] {null, "/topic/other", "/topic/chat-rooms/42/other"}) {
            StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            headers.setDestination(destination);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

            assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, channel));
        }
    }

    @Test
    void sendOnlyAllowsApplicationDestinations() {
        for (String destination : new String[] {null, "/topic/chat-rooms/42"}) {
            StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
            headers.setDestination(destination);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

            assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, channel));
        }

        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/chat-rooms/42/messages");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        interceptor.preSend(message, channel);
    }

    @Test
    void connectionWithoutPrincipalIsRejected() {
        when(userResolver.requireUser(null)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThrows(BusinessException.class, () -> interceptor.preSend(message, channel));
    }
}
