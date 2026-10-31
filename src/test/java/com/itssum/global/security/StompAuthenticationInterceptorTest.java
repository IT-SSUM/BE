package com.itssum.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class StompAuthenticationInterceptorTest {

    private static final JwtProperties PROPERTIES = new JwtProperties(
            "test-jwt-secret-0123456789-abcdefghijklmnop", Duration.ofMinutes(30), Duration.ofDays(14));

    private final JwtConfig jwtConfig = new JwtConfig();
    private final AccessTokenProvider tokenProvider =
            new AccessTokenProvider(jwtConfig.jwtEncoder(PROPERTIES), PROPERTIES);
    private final StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(
            jwtConfig.jwtDecoder(PROPERTIES), jwtConfig.jwtAuthenticationConverter());
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void connectWithValidTokenSetsUserIdAsPrincipal() {
        Message<byte[]> message = connect("Bearer " + tokenProvider.issue(10L, UserRole.DONOR));

        interceptor.preSend(message, channel);

        assertEquals("10", StompHeaderAccessor.wrap(message).getUser().getName());
    }

    @Test
    void connectWithoutValidBearerTokenIsRejected() {
        for (String authorization : new String[] {null, "Bearer not.a.jwt", "Basic abc"}) {
            Message<byte[]> message = connect(authorization);

            BusinessException e = assertThrows(BusinessException.class, () -> interceptor.preSend(message, channel));
            assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
        }
    }

    @Test
    void otherFramesPassThroughUntouched() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/chat-rooms/1/messages");
        headers.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertSame(message, interceptor.preSend(message, channel));
        assertNull(StompHeaderAccessor.wrap(message).getUser());
    }

    private Message<byte[]> connect(String authorization) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            headers.addNativeHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        // 실제 STOMP 인바운드 메시지처럼 헤더를 수정 가능하게 둔다
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
