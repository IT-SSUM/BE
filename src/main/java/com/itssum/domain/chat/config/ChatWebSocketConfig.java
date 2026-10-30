package com.itssum.domain.chat.config;

import com.itssum.global.security.StompAuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthenticationInterceptor stompAuthenticationInterceptor;
    private final ChatStompInterceptor chatStompInterceptor;

    @Value("${chat.websocket.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws");
        if (!allowedOrigins.isBlank()) {
            endpoint.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim).toArray(String[]::new));
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 인증이 먼저 세션 사용자를 넣어야 채팅 권한 검사가 그 값을 본다
        registration.interceptors(stompAuthenticationInterceptor, chatStompInterceptor);
    }
}
