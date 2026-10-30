package com.itssum.global.security;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 프레임의 Authorization: Bearer 토큰을 검증해 세션 사용자로 넣는다.
 * 사용자 이름은 sub(회원 ID)라서 ChatUserResolver 가 그대로 쓴다. 연결된 세션은 토큰이 만료돼도 유지된다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            accessor.setUser(jwtAuthenticationConverter.convert(
                    jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()))));
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return message;
    }
}
